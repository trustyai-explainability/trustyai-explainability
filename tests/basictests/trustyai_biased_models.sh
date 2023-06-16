#!/bin/bash

source $TEST_DIR/common

MY_DIR=$(readlink -f `dirname "${BASH_SOURCE[0]}"`)

source ${MY_DIR}/../util
RESOURCEDIR="${MY_DIR}/../resources"

TEST_USER=${OPENSHIFT_TESTUSER_NAME:-"admin"} #Username used to login to the ODH Dashboard
TEST_PASS=${OPENSHIFT_TESTUSER_PASS:-"admin"} #Password used to login to the ODH Dashboard
OPENSHIFT_OAUTH_ENDPOINT="https://$(oc get route -n openshift-authentication   oauth-openshift -o json | jq -r '.spec.host')"
MM_NAMESPACE="${ODHPROJECT}-model"

MODEL_ALPHA=demo-loan-nn-onnx-alpha
MODEL_BETA=demo-loan-nn-onnx-beta

os::test::junit::declare_suite_start "$MY_SCRIPT"

function get_authentication(){
  header "Getting authentication credentials to cluster"
  oc adm policy add-role-to-user view -n ${ODHPROJECT} --rolebinding-name "view-$TEST_USER" $TEST_USER
  TESTUSER_BEARER_TOKEN="$(curl -kiL -u $TEST_USER:$TEST_PASS -H 'X-CSRF-Token: xxx' $OPENSHIFT_OAUTH_ENDPOINT'/oauth/authorize?response_type=token&client_id=openshift-challenging-client' | grep -oP 'access_token=\K[^&]*')"
}

function check_trustyai_resources() {
  header "Checking that TrustyAI resources have spun up"
  oc project $ODHPROJECT
  os::cmd::try_until_text "oc get deployment modelmesh-controller" "modelmesh-controller" $odhdefaulttimeout $odhdefaultinterval
  os::cmd::try_until_text "oc get deployment trustyai-service" "trustyai-service" $odhdefaulttimeout $odhdefaultinterval
  os::cmd::try_until_text "oc get route trustyai-service-route" "trustyai-service-route" $odhdefaulttimeout $odhdefaultinterval



  oc wait --for=condition=Ready $(oc get pod -o name | grep trustyai) --timeout=${odhdefaulttimeout}ms
}

function deploy_model() {
    header "Deploying models into ModelMesh"
    oc new-project $MM_NAMESPACE || true
    os::cmd::expect_success "oc project $MM_NAMESPACE"
    oc label namespace $MM_NAMESPACE "modelmesh-enabled=true" --overwrite=true || echo "Failed to apply modelmesh-enabled label."
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/secret.yaml -n ${MM_NAMESPACE}"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/ovms-1.x.yaml  -n ${MM_NAMESPACE}"

    SECRETKEY=$(openssl rand -hex 32)
    sed -i "s/<secretkey>/$SECRETKEY/g" ${RESOURCEDIR}/trustyai/sample-minio.yaml
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/sample-minio.yaml -n ${MM_NAMESPACE}"

    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/models/model0_onnx.yaml  -n ${MM_NAMESPACE}"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/models/model1_onnx.yaml  -n ${MM_NAMESPACE}"


}

function check_mm_resources() {
  header "Checking that ModelMesh resources have spun up"
  oc project $MM_NAMESPACE
  os::cmd::try_until_text "oc get route demo-loan-nn-onnx-alpha" "demo-loan-nn-onnx-alpha" $odhdefaulttimeout $odhdefaultinterval
  os::cmd::try_until_text "oc get route demo-loan-nn-onnx-beta" "demo-loan-nn-onnx-beta" $odhdefaulttimeout $odhdefaultinterval

  os::cmd::try_until_text "oc get pod | grep modelmesh-serving" "5/5" $odhdefaulttimeout $odhdefaultinterval

  INFER_ROUTE_ALPHA=$(oc get route demo-loan-nn-onnx-alpha --template={{.spec.host}}{{.spec.path}})
  INFER_ROUTE_BETA=$(oc get route demo-loan-nn-onnx-alpha --template={{.spec.host}}{{.spec.path}})
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE_ALPHA/infer -d @${RESOURCEDIR}/data/loan_default/dummy_data.json" "model_name"
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE_BETA/infer -d @${RESOURCEDIR}/data/loan_default/dummy_data.json" "model_name"
  echo "Waiting 10s for endpoints to stabilize"
  sleep 10
}


function check_communication(){
    header "Check communication between TrustyAI and ModelMesh"
    oc project $MM_NAMESPACE

    # send some data to modelmesh
    oc project ${ODHPROJECT}
    os::cmd::try_until_text "oc logs $(oc get pods -o name | grep trustyai-service)" "Received partial input payload" $odhdefaulttimeout $odhdefaultinterval
}

function send_data(){
    header "Sending some data for TrustyAI (this will take a minute or two)"
    oc project $MM_NAMESPACE

    $RESOURCEDIR/utils/send_data_batch $RESOURCEDIR/data/loan_default/batch_01.json
}

function schedule_and_check_request(){
  header "Create a metric request and confirm calculation"
  oc project $ODHPROJECT
  TRUSTY_ROUTE=$(oc get route/trustyai --template={{.spec.host}})

  for METRIC_NAME in "spd" "dir"
  do
    METRIC_UPPERCASE=$(echo ${METRIC_NAME} | tr '[:lower:]' '[:upper:]')
    for MODEL in $MODEL_ALPHA $MODEL_BETA
    do
      curl -s --location http://$TRUSTY_ROUTE/metrics/$METRIC_NAME/request \
        --header 'Content-Type: application/json' \
        --data "{
                  \"modelId\": \"$MODEL\",
                  \"protectedAttribute\": \"input-3\",
                  \"favorableOutcome\":  0,
                  \"outcomeName\": \"output-0\",
                  \"privilegedAttribute\": 1.0,
                  \"unprivilegedAttribute\": 0.0
                }"
      echo ": Registered real-time monitoring of $METRIC_UPPERCASE"
    done
  done
}

function send_more_data(){
    header "Sending some data for TrustyAI (this will take a minute or two)"
    oc project $MM_NAMESPACE

  LOOP_IDX=1
  for batch in $(ls $RESOURCEDIR/data/loan_default/)
  do
      if [ "$batch" = "training_data.json" ]; then
        :
      elif [ "$batch" = "batch_01.json" ]; then
        :
      elif [ "$batch" = "dummy_data.json" ]; then
         :
      else
        echo "===== Deployment Day $LOOP_IDX ====="
        $RESOURCEDIR/utils/send_data_batch $batch

        for i in {1..5}
        do
          echo -ne "\rSleeping for rest of day...$((5 - $i))"
          sleep 1
        done
        echo
      fi
      let "LOOP_IDX++"
    done
}


function test_prometheus_scraping(){
    header "Ensure metrics are in Prometheus"
    MODEL_MONITORING_ROUTE=$(oc get route -n ${ODHPROJECT} odh-model-monitoring --template={{.spec.host}})
    os::cmd::try_until_text "curl -k --location -g --request GET 'https://'$MODEL_MONITORING_ROUTE'//api/v1/query?query=trustyai_spd' -H 'Authorization: Bearer $TESTUSER_BEARER_TOKEN' -i" "value" $odhdefaulttimeout $odhdefaultinterval
    os::cmd::try_until_text "curl -k --location -g --request GET 'https://'$MODEL_MONITORING_ROUTE'//api/v1/query?query=trustyai_dir' -H 'Authorization: Bearer $TESTUSER_BEARER_TOKEN' -i" "value" $odhdefaulttimeout $odhdefaultinterval
}


function teardown_trustyai_test() {
  header "Cleaning up the TrustyAI test"
  oc project $ODHPROJECT

  REQUEST_ID="$(curl http://$TRUSTY_ROUTE/metrics/spd/requests | jq '.requests [0].id')"

  # delete all requests
  for METRIC_NAME in "spd" "dir"
  do
    for REQUEST in $(curl -s http://$TRUSTY_ROUTE/metrics/$METRIC_NAME/requests | jq -r '.requests [].id')
    do
      echo -n $REQUEST": "
      curl -X DELETE --location http://$TRUSTY_ROUTE/metrics/$METRIC_NAME/request \
          -H 'Content-Type: application/json' \
          -d "{
                \"requestId\": \"$REQUEST\"
              }"
      echo
    done
  done

  oc project $MM_NAMESPACE
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/secret.yaml"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/ovms-1.x.yaml"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/models/model0_onnx.yaml"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/models/model1_onnx.yaml"
  os::cmd::expect_success "oc delete project $MM_NAMESPACE"
}



get_authentication
check_trustyai_resources
deploy_model
check_mm_resources
check_communication
send_data
schedule_and_check_request
test_prometheus_scraping
teardown_trustyai_test

os::test::junit::declare_suite_end