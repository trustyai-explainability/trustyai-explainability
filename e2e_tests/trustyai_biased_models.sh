#!/bin/bash

source util/constants
source util/unit_testing_functions

MODEL_ALPHA=demo-loan-nn-onnx-alpha
MODEL_BETA=demo-loan-nn-onnx-beta

os::test::junit::declare_suite_start "$MY_SCRIPT"

function setup_test() {
  header "Installing ODH and creating project"
  oc new-project $ODHPROJECT
  oc project $ODHPROJECT
  os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/common/odh-core.yaml"
}

function check_trustyai_resources() {
  header "Checking that TrustyAI resources have spun up"
  oc project $ODHPROJECT
  os::cmd::try_until_text "oc get deployment modelmesh-controller" "modelmesh-controller" $odhdefaulttimeout $odhdefaultinterval
  os::cmd::try_until_text "oc get deployment trustyai-service" "trustyai-service" $odhdefaulttimeout $odhdefaultinterval
  os::cmd::try_until_text "oc get route trustyai-service-route" "trustyai-service-route" $odhdefaulttimeout $odhdefaultinterval

  echo -n "Waiting on trustyai pod to spin up "
  while [[ -z "$(oc get pods | grep trustyai-service | grep 1/1)" ]]
  do
    echo -n "."
    sleep 5
  done
  echo "[done]"
}

function deploy_model() {
    header "Deploying model into ModelMesh"
    oc new-project $MM_NAMESPACE
    os::cmd::expect_success "oc project $MM_NAMESPACE"
    oc label namespace $MM_NAMESPACE "modelmesh-enabled=true" --overwrite=true || echo "Failed to apply modelmesh-enabled label."
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/common/secret.yaml -n ${MM_NAMESPACE}"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/biased_models/ovms-1.x.yaml  -n ${MM_NAMESPACE}"

    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/biased_models/model0_onnx.yaml  -n ${MM_NAMESPACE}"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/biased_models/model1_onnx.yaml  -n ${MM_NAMESPACE}"

    echo -n "Waiting on modelserving runtime pods to spin up "
    while [[ -z "$(oc get pods | grep modelmesh-serving | grep 5/5)" ]]
    do
      echo -n "."
      sleep 5
    done
    echo "[done]"
}

function check_mm_resources() {
  header "Checking that ModelMesh resources have spun up"
  oc project $MM_NAMESPACE
  os::cmd::try_until_text "oc get route demo-loan-nn-onnx-alpha" "demo-loan-nn-onnx-alpha" $odhdefaulttimeout $odhdefaultinterval
  os::cmd::try_until_text "oc get route demo-loan-nn-onnx-beta" "demo-loan-nn-onnx-beta" $odhdefaulttimeout $odhdefaultinterval

  os::cmd::try_until_text "oc get pod | grep modelmesh-serving" "5/5" $odhdefaulttimeout $odhdefaultinterval

  INFER_ROUTE_ALPHA=$(oc get route demo-loan-nn-onnx-alpha --template={{.spec.host}}{{.spec.path}})
  INFER_ROUTE_BETA=$(oc get route demo-loan-nn-onnx-alpha --template={{.spec.host}}{{.spec.path}})
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE_ALPHA/infer -d @${RESOURCEDIR}/biased_models/dummy_data.json" "model_name"
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE_BETA/infer -d @${RESOURCEDIR}/biased_models/dummy_data.json" "model_name"
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

    $RESOURCEDIR/biased_models/send_data_batch batch_01.json
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
                  \"favorableOutcome\": {
                    \"type\": \"INT32\",
                    \"value\": 0
                  },
                  \"outcomeName\": \"output-0\",
                  \"privilegedAttribute\": {
                    \"type\": \"DOUBLE\",
                    \"value\": 1.0
                  },
                  \"unprivilegedAttribute\": {
                    \"type\": \"DOUBLE\",
                    \"value\": 0.0
                  }
                }"
      echo ": Registered real-time monitoring of $METRIC_UPPERCASE"
    done
  done
}

function send_more_data(){
    header "Sending some data for TrustyAI (this will take a minute or two)"
    oc project $MM_NAMESPACE

  LOOP_IDX=1
  for batch in $(ls $RESOURCEDIR/biased_models/data_json_combined/)
  do
      if [ "$batch" = "training_data.json" ]; then
        :
      elif [ "$batch" = "batch_01.json" ]; then
        :
      else
        echo "===== Deployment Day $LOOP_IDX ====="
        $RESOURCEDIR/biased_models/send_data_batch $batch

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
    echo "Please check that "trustyai_spd" data exists at the following endpoint:"
    echo $MODEL_MONITORING_ROUTE
    echo ""
    echo "Press enter once verified..."
    read  -n 1
}


function teardown_trustyai_test() {
  header "Cleaning up the TrustyAI test"
  oc project $ODHPROJECT

  REQUEST_ID="$(curl http://$TRUSTY_ROUTE/metrics/spd/requests | jq '.requests [0].id')"

  os::cmd::expect_success_and_text "curl -X DELETE --location http://$TRUSTY_ROUTE/metrics/spd/request \
    -H 'Content-Type: application/json' \
    -d '{
          \"requestId\": \"'"$REQUEST_ID"'\"
        }'" "Removed"

    os::cmd::expect_success_and_text "curl -X DELETE --location http://$TRUSTY_ROUTE/metrics/dir/request \
      -H 'Content-Type: application/json' \
      -d '{
            \"requestId\": \"'"$REQUEST_ID"'\"
          }'" "Removed"

  oc project $MM_NAMESPACE
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/common/secret.yaml"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/biased_models/ovms-1.x.yaml"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/biased_models/model0_onnx.yaml"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/biased_models/model1_onnx.yaml"
  os::cmd::expect_success "oc delete project $MM_NAMESPACE"
}

function teardown_test() {
  oc project $ODHPROJECT
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/biased_models/odh-core.yaml"
  os::cmd::expect_success "oc delete project $ODHPROJECT"
}


if [ ! -z $1 ] && [ $1 == "clean" ]; then
  teardown_trustyai_test
elif [ ! -z $1 ] && [ $1 == "reset" ]; then
  teardown_trustyai_test
  teardown_test
else
  setup_test
  check_trustyai_resources
  deploy_model
  check_mm_resources
  check_communication
  send_data
  schedule_and_check_request
  send_more_data
  test_prometheus_scraping
fi




