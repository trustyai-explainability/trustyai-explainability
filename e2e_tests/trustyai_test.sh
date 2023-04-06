#!/bin/bash

source util/constants
source util/unit_testing_functions

RESOURCEDIR="resources"
MM_NAMESPACE="${ODHPROJECT}-model"

os::test::junit::declare_suite_start "$MY_SCRIPT"


function setup_test() {
  header "Installing ODH and creating project"
  oc new-project $ODHPROJECT
  oc project $ODHPROJECT
  os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/odh-core.yaml"
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
    header "Deploying model into ModelMesh"
    oc new-project $MM_NAMESPACE
    os::cmd::expect_success "oc project $MM_NAMESPACE"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/service_account.yaml -n ${MM_NAMESPACE}"
    oc label namespace $MM_NAMESPACE "modelmesh-enabled=true" --overwrite=true || echo "Failed to apply modelmesh-enabled label."
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/secret.yaml -n ${MM_NAMESPACE}"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/odh-mlserver-0.x.yaml  -n ${MM_NAMESPACE}"

    SECRETKEY=$(openssl rand -hex 32)
    sed -i "s/<secretkey>/$SECRETKEY/g" ${RESOURCEDIR}/sample-minio.yaml
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/sample-minio.yaml -n ${MM_NAMESPACE}"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/openvino-inference-service.yaml -n ${MM_NAMESPACE}"
    echo "Sleeping for 30s to allow models to download..."
    sleep 30
}

function check_mm_resources() {
  header "Checking that ModelMesh resources have spun up"
  oc project $MM_NAMESPACE
  os::cmd::try_until_text "oc get route example-sklearn-isvc" "example-sklearn-isvc" $odhdefaulttimeout $odhdefaultinterval
  INFER_ROUTE=$(oc get route example-sklearn-isvc --template={{.spec.host}}{{.spec.path}})
  token=$(oc create token user-one -n ${MM_NAMESPACE})
  os::cmd::try_until_text "oc get pod | grep modelmesh-serving" "5/5" $odhdefaulttimeout $odhdefaultinterval
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/data.json -H 'Authorization: Bearer $token' -i" "model_name"
}

function check_communication(){
    header "Check communication between TrustyAI and ModelMesh"
    oc project $MM_NAMESPACE

    # send some data to modelmesh
    os::cmd::expect_success_and_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/data.json -H 'Authorization: Bearer $token' -i" "model_name"
    oc project ${ODHPROJECT}
    os::cmd::try_until_text "oc logs $(oc get pods -o name | grep trustyai-service)" "Received partial input payload" $odhdefaulttimeout $odhdefaultinterval
}

function generate_data(){
    header "Generate some data for TrustyAI (this will take a minute or two)"
    oc project $MM_NAMESPACE

    # send a bunch of random data to the model
    DIVISOR=128.498 # divide bash's $RANDOM by this to get a float range of [0.,255.], for MNIST
    for i in {1..500};
    do
      DATA=$(sed "s/\[40.83, 3.5, 0.5, 0\]/\[$(($RANDOM % 2)),$(($RANDOM / 128)),$(($RANDOM / 128)), $(($RANDOM / 128)) \]/" ${RESOURCEDIR}/data.json)
      curl -k https://$INFER_ROUTE/infer -d "$DATA"  -H 'Authorization: Bearer $token' -i > /dev/null 2>&1 &
      sleep .01
    done
}

function schedule_and_check_request(){
  header "Create a metric request and confirm calculation"
  oc project $ODHPROJECT
  TRUSTY_ROUTE=$(oc get route/trustyai --template={{.spec.host}})

  os::cmd::expect_success_and_text "curl --location http://$TRUSTY_ROUTE/metrics/spd/request \
    --header 'Content-Type: application/json' \
    --data '{
        \"modelId\": \"example-sklearn-isvc\",
        \"protectedAttribute\": \"input-0\",
        \"favorableOutcome\": {
            \"type\": \"INT64\",
            \"value\": 0.0
        },
        \"outcomeName\": \"output-0\",
        \"privilegedAttribute\": {
            \"type\": \"DOUBLE\",
            \"value\": 0.0
        },
        \"unprivilegedAttribute\": {
            \"type\": \"DOUBLE\",
            \"value\": 1.0
        }
    }'" "requestId"
  os::cmd::try_until_text "curl http://$TRUSTY_ROUTE/q/metrics" "trustyai_spd"
}


function test_prometheus_scraping(){
    header "Ensure metrics are in Prometheus"
    echo "Sleeping 30s to allow for metric calculation"
    sleep 30
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

  oc project $MM_NAMESPACE
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/secret.yaml"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/odh-mlserver-0.x.yaml"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/model.yaml"
  os::cmd::expect_success "oc delete project $MM_NAMESPACE"
}

function teardown_test() {
  oc project $ODHPROJECT
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/odh-core.yaml"
  os::cmd::expect_success "oc delete project $ODHPROJECT"
}

setup_test
deploy_model
check_mm_resources
check_communication
generate_data
schedule_and_check_request
test_prometheus_scraping
teardown_trustyai_test
teardown_test



