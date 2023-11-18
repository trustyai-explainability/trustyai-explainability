#!/bin/bash

source $TEST_DIR/common

MY_DIR=$(readlink -f `dirname "${BASH_SOURCE[0]}"`)

source ${MY_DIR}/../util
RESOURCEDIR="${MY_DIR}/../resources"

TEST_USER=${OPENSHIFT_TESTUSER_NAME:-"admin"} #Username used to login to the ODH Dashboard
TEST_PASS=${OPENSHIFT_TESTUSER_PASS:-"admin"} #Password used to login to the ODH Dashboard
LOCAL=${LOCAL:-false}
TEARDOWN=${TEARDOWN:-false}
MM_NAMESPACE="${ODHPROJECT}-model"

MODEL_ALPHA=demo-loan-nn-onnx-alpha
MODEL_BETA=demo-loan-nn-onnx-beta


# trackers of test successes
REQUESTS_CREATED=false
FAILURE=false
FAILURE_HANDLING='FAILURE=true && echo -e "\033[0;31mERROR\033[0m"'

# Authentication token
TOKEN=$(oc whoami -t)

os::test::junit::declare_suite_start "$MY_SCRIPT"

function setup_monitoring() {
    header "Enabling User Workload Monitoring on the cluster"
    oc apply -f ${RESOURCEDIR}/modelmesh/enable-uwm.yaml || eval "$FAILURE_HANDLING"
}


function install_trustyai_operator(){
  header "Installing TrustyAI Operator"
  oc project $ODHPROJECT || eval "$FAILURE_HANDLING"

  oc apply -f ${RESOURCEDIR}/trustyai/trustyai_operator_kfdef.yaml || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get deployment trustyai-operator" "trustyai-operator" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get pods | grep trustyai-service-operator" "Running" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
}

function deploy_model() {
    header "Deploying models into ModelMesh"
    oc new-project $MM_NAMESPACE || true
    os::cmd::expect_success "oc project $MM_NAMESPACE"
    oc label namespace $MM_NAMESPACE "modelmesh-enabled=true" --overwrite=true || echo "Failed to apply modelmesh-enabled label."  || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/secret.yaml -n ${MM_NAMESPACE}"  || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/ovms-1.x.yaml  -n ${MM_NAMESPACE}"  || eval "$FAILURE_HANDLING"

    SECRETKEY=$(openssl rand -hex 32)
    sed -i "s/<secretkey>/$SECRETKEY/g" ${RESOURCEDIR}/trustyai/sample-minio.yaml || eval "$FAILURE_HANDLING"  || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/sample-minio.yaml -n ${MM_NAMESPACE}"  || eval "$FAILURE_HANDLING"

    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/models/model0_onnx.yaml  -n ${MM_NAMESPACE}"  || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/models/model1_onnx.yaml  -n ${MM_NAMESPACE}"  || eval "$FAILURE_HANDLING"
    echo "Waiting 30s for model deployments to stabilize"
    sleep 30
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/trustyai_crd.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
}

function check_trustyai_resources() {
  header "Checking that TrustyAI resources have spun up"
  oc project $MM_NAMESPACE  || eval "$FAILURE_HANDLING"

  os::cmd::try_until_text "oc get deployment trustyai-service" "trustyai-service" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get route trustyai-service-route" "trustyai-service-route" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get pod | grep trustyai-service" "2/2" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"

}

function check_trustyai_conditions() {
  header "Checking the TrustyAIService conditions are correct"
  oc project $MM_NAMESPACE  || eval "$FAILURE_HANDLING"

  os::cmd::try_until_text "oc get TrustyAIService trustyai-service -o json | jq -r '.status.conditions[] | select(.type==\"InferenceServicesPresent\") | \"\(.type),\(.status),\(.reason),\(.message)\"'" "InferenceServicesPresent,True,InferenceServicesFound,InferenceServices found" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get TrustyAIService trustyai-service -o json | jq -r '.status.conditions[] | select(.type==\"PVCAvailable\") | \"\(.type),\(.status),\(.reason),\(.message)\"'" "PVCAvailable,True,PVCFound,PersistentVolumeClaim found" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get TrustyAIService trustyai-service -o json | jq -r '.status.conditions[] | select(.type==\"RouteAvailable\") | \"\(.type),\(.status),\(.reason),\(.message)\"'" "RouteAvailable,True,RouteFound,Route found" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get TrustyAIService trustyai-service -o json | jq -r '.status.conditions[] | select(.type==\"Available\") | \"\(.type),\(.status),\(.reason),\(.message)\"'" "Available,True,AllComponentsReady,AllComponentsReady" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"

}

function check_trustyai_authentication() {
  header "Checking the TrustyAIService authentication"
  oc project $MM_NAMESPACE  || eval "$FAILURE_HANDLING"
  TRUSTY_ROUTE=https://$(oc get route/trustyai-service --template={{.spec.host}}) || eval "$FAILURE_HANDLING"

  os::cmd::try_until_text "curl -k -s -o /dev/null -w \"%{http_code}\" ${TRUSTY_ROUTE}/info" "403" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "curl -k -s -o /dev/null -w \"%{http_code}\" -H \"Authorization: Bearer ${TOKEN}\" ${TRUSTY_ROUTE}/info" "200" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
}

function check_mm_resources() {
  header "Checking that ModelMesh resources have spun up"
  oc project $MM_NAMESPACE
  os::cmd::try_until_text "oc get pod | grep modelmesh-serving" "5/5" $odhdefaulttimeout $odhdefaultinterval  || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get route demo-loan-nn-onnx-alpha" "demo-loan-nn-onnx-alpha" $odhdefaulttimeout $odhdefaultinterval  || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get route demo-loan-nn-onnx-beta" "demo-loan-nn-onnx-beta" $odhdefaulttimeout $odhdefaultinterval  || eval "$FAILURE_HANDLING"

  INFER_ROUTE_ALPHA=$(oc get route demo-loan-nn-onnx-alpha --template={{.spec.host}}{{.spec.path}}) || eval "$FAILURE_HANDLING"
  INFER_ROUTE_BETA=$(oc get route demo-loan-nn-onnx-alpha --template={{.spec.host}}{{.spec.path}}) || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE_ALPHA/infer -d @${RESOURCEDIR}/data/loan_default/dummy_data.json" "model_name"  || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE_BETA/infer -d @${RESOURCEDIR}/data/loan_default/dummy_data.json" "model_name"  || eval "$FAILURE_HANDLING"
  echo "Waiting 10s for endpoints to stabilize"
  sleep 10
}

function check_communication(){
    header "Check communication between TrustyAI and ModelMesh"
    oc project $MM_NAMESPACE

    # send some data to modelmesh
    os::cmd::try_until_text "curl -k https://$INFER_ROUTE_ALPHA/infer -d @${RESOURCEDIR}/data/loan_default/dummy_data.json; oc logs $(oc get pods -o name | grep trustyai-service)" "Received partial input payload" $odhdefaulttimeout $odhdefaultinterval  || eval "$FAILURE_HANDLING"
}

function send_data(){
    header "Sending some data for TrustyAI (this will take a minute or two)"
    oc project $MM_NAMESPACE
    $RESOURCEDIR/utils/send_data_batch $RESOURCEDIR/data/loan_default_batched/batch_01.json || eval "$FAILURE_HANDLING"
}


function schedule_and_check_request(){
  header "Create a metric request and confirm calculation"
  oc project $MM_NAMESPACE  || eval "$FAILURE_HANDLING"
  TRUSTY_ROUTE=https://$(oc get route/trustyai-service --template={{.spec.host}}) || eval "$FAILURE_HANDLING"

  for METRIC_NAME in "spd" "dir"
  do
    METRIC_UPPERCASE=$(echo ${METRIC_NAME} | tr '[:lower:]' '[:upper:]')
    for MODEL in $MODEL_ALPHA $MODEL_BETA
    do
      curl -sk -H "Authorization: Bearer ${TOKEN}" --location $TRUSTY_ROUTE/metrics/$METRIC_NAME/request \
        --header 'Content-Type: application/json' \
        --data "{
                  \"modelId\": \"$MODEL\",
                  \"protectedAttribute\": \"customer_data_input-3\",
                  \"favorableOutcome\":  0,
                  \"outcomeName\": \"predict\",
                  \"privilegedAttribute\": 1.0,
                  \"unprivilegedAttribute\": 0.0
                }" || eval "$FAILURE_HANDLING"
      echo ": Registered real-time monitoring of $METRIC_UPPERCASE"
    done
  done

  [ $FAILURE = false ] && REQUESTS_CREATED=true
}

# this function may be useful if we want to vary the metric values over the course of the test, so preserving just in case
# ========
#function send_more_data(){
#    header "Sending some data for TrustyAI (this will take a minute or two)"
#    oc project $MM_NAMESPACE
#
#  LOOP_IDX=1
#  for batch in "$RESOURCEDIR"/data/loan_default_batched/*.json
#  do
#      if [ "$batch" = "training_data.json" ]; then
#        :
#      elif [ "$batch" = "batch_01.json" ]; then
#        :
#      elif [ "$batch" = "dummy_data.json" ]; then
#         :
#      else
#        echo "===== Deployment Day $LOOP_IDX ====="
#        "$RESOURCEDIR"/utils/send_data_batch "$batch" || eval "$FAILURE_HANDLING"
#
#        for i in {1..5}
#        do
#          echo -ne "\rSleeping for rest of day...$((5 - $i))"
#          sleep 1
#        done
#        echo
#      fi
#      let "LOOP_IDX++"
#    done
#}


function test_prometheus_scraping(){
    header "Ensure metrics are in Prometheus"

    SECRET=`oc get secret -n openshift-user-workload-monitoring | grep  prometheus-user-workload-token | head -n 1 | awk '{print $1 }'` || eval "$FAILURE_HANDLING"
    TOKEN=`echo $(oc get secret $SECRET -n openshift-user-workload-monitoring -o json | jq -r '.data.token') | base64 -d` || eval "$FAILURE_HANDLING"
    THANOS_QUERIER_HOST=`oc get route thanos-querier -n openshift-monitoring -o json | jq -r '.spec.host'` || eval "$FAILURE_HANDLING"
    os::cmd::try_until_text "curl -X GET -kG \"https://$THANOS_QUERIER_HOST/api/v1/query?\" --data-urlencode \"query=trustyai_spd{namespace='opendatahub-model'}\" -H 'Authorization: Bearer $TOKEN' | jq '.data.result[0].metric.protected'" "customer_data_input-3" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
    os::cmd::try_until_text "curl -X GET -kG \"https://$THANOS_QUERIER_HOST/api/v1/query?\" --data-urlencode \"query=trustyai_dir{namespace='opendatahub-model'}\" -H 'Authorization: Bearer $TOKEN' | jq '.data.result[0].metric.protected'" "customer_data_input-3" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
}

function local_teardown_wait(){
  header "Local test suite finished, pausing before teardown for any manual cluster inspection"
  echo -n "Hit enter to begin teardown: "; read
}

function teardown_trustyai_test() {
  header "Cleaning up the TrustyAI test"
  oc project $MM_NAMESPACE  || eval "$FAILURE_HANDLING"
  oc get pods >> ${ARTIFACT_DIR}/${MM_NAMESPACE}.pods.txt
  oc get events >>  ${ARTIFACT_DIR}/${MM_NAMESPACE}.events.txt
  oc logs -n opendatahub $(oc get pods -o name -n opendatahub | grep trustyai) >> ${ARTIFACT_DIR}/${ODHPROJECT}.trustyoperatorlogs.txt
  oc logs $(oc get pods -o name | grep trustyai) >> ${ARTIFACT_DIR}/trusty_service_pod_logs.txt || true
  oc exec $(oc get pods -o name | grep trustyai) -c trustyai-service -- bash -c "ls /inputs/" >> ${ARTIFACT_DIR}/trusty_service_inputs_ls.txt || true
  
  TRUSTY_ROUTE=http://$(oc get route/trustyai-service --template={{.spec.host}}) || eval "$FAILURE_HANDLING"

  # delete all requests
  if [ $REQUESTS_CREATED = true ]; then
    for METRIC_NAME in "spd" "dir"
    do
      for REQUEST in $(curl -sk -H "Authorization: Bearer ${TOKEN}" $TRUSTY_ROUTE/metrics/$METRIC_NAME/requests | jq -r '.requests [].id')
      do
        echo -n $REQUEST": "
        curl -k  -H "Authorization: Bearer ${TOKEN}" -X DELETE --location $TRUSTY_ROUTE/metrics/$METRIC_NAME/request \
            -H 'Content-Type: application/json' \
            -d "{
                  \"requestId\": \"$REQUEST\"
                }"
        echo
      done
    done
  fi


  oc project $MM_NAMESPACE || echo "Could not switch to $MM_NAMESPACE"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/secret.yaml"  || eval "$FAILURE_HANDLING"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/ovms-1.x.yaml"  || eval "$FAILURE_HANDLING"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/trustyai_crd.yaml"   || eval "$FAILURE_HANDLING"
  os::cmd::expect_success "oc delete project $MM_NAMESPACE" || true

  oc project $ODHPROJECT || eval "$FAILURE_HANDLING"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/trustyai_operator_kfdef.yaml"  || eval "$FAILURE_HANDLING"
  oc delete deployment trustyai-service-operator-controller-manager  || echo "No trustyai operator deployment found"
}

if [ $TEARDOWN = false ]; then
  setup_monitoring
  [ $FAILURE = false ] && install_trustyai_operator              || echo -e "\033[0;31mSkipping TrustyAI-Operator install due to previous failure\033[0m"
  [ $FAILURE = false ] && deploy_model                  || echo -e "\033[0;31mSkipping model deployment due to previous failure\033[0m"
  [ $FAILURE = false ] && check_trustyai_resources      || echo -e "\033[0;31mSkipping TrustyAI resource check due to previous failure\033[0m"
  [ $FAILURE = false ] && check_trustyai_conditions     || echo -e "\033[0;31mSkipping TrustyAI conditions check due to previous failure\033[0m"
  [ $FAILURE = false ] && check_trustyai_authentication || echo -e "\033[0;31mSkipping TrustyAI authentication check due to previous failure\033[0m"
  [ $FAILURE = false ] && check_mm_resources            || echo -e "\033[0;31mSkipping ModelMesh resource check due to previous failure\033[0m"
  [ $FAILURE = false ] && check_communication           || echo -e "\033[0;31mSkipping ModelMesh-TrustyAI communication check due to previous failure\033[0m"
  [ $FAILURE = false ] && send_data                     || echo -e "\033[0;31mSkipping data generation due to previous failure\033[0m"
  [ $FAILURE = false ] && schedule_and_check_request    || echo -e "\033[0;31mSkipping metric scheduling due to previous failure\033[0m\033[0m"
  [ $FAILURE = false ] && test_prometheus_scraping      || echo -e "\033[0;31mSkipping Prometheus data check due to previous failure\033[0m\033[0m"

  [ $LOCAL = true ] && local_teardown_wait
fi
teardown_trustyai_test

[ $FAILURE = true ] && os::cmd::expect_success "echo 'A previous assertion failed, marking suite as failed' && exit 1"

os::test::junit::declare_suite_end
