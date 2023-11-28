#!/bin/bash

source $TEST_DIR/common

MY_DIR=$(readlink -f `dirname "${BASH_SOURCE[0]}"`)

source ${MY_DIR}/../util
RESOURCEDIR="${MY_DIR}/../resources"

TEST_USER=${OPENSHIFT_TESTUSER_NAME:-"admin"} #Username used to login
TEST_PASS=${OPENSHIFT_TESTUSER_PASS:-"admin"} #Password used to login
OPENSHIFT_OAUTH_ENDPOINT="https://$(oc get route -n openshift-authentication   oauth-openshift -o json | jq -r '.spec.host')"

LOCAL=${LOCAL:-false}
TEARDOWN=${TEARDOWN:-false}

MM_NAMESPACE="${ODHPROJECT}-model"

# trackers of test successes
REQUESTS_CREATED=false
FAILURE=false
FAILURE_HANDLING='FAILURE=true && echo -e "\033[0;31mERROR\033[0m"'

# Authentication token
TOKEN="$(curl -skiL -u $TEST_USER:$TEST_PASS -H 'X-CSRF-Token: xxx' '$OPENSHIFT_OAUTH_ENDPOINT/oauth/authorize?response_type=token&client_id=openshift-challenging-client' | grep -oP 'access_token=\K[^&]*')"

os::test::junit::declare_suite_start "$MY_SCRIPT"

# Function to add the Authorization token to curl commands
function curl_token() {
    curl -H "Authorization: Bearer ${TOKEN}" "$@"
}

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
    header "Deploying model into ModelMesh"
    oc new-project $MM_NAMESPACE || true

    os::cmd::expect_success "oc project $MM_NAMESPACE" || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/modelmesh/service_account.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
    oc label namespace $MM_NAMESPACE "modelmesh-enabled=true" --overwrite=true || echo "Failed to apply modelmesh-enabled label."
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/secret.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/odh-mlserver-1.x.yaml  -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
#    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/model.yaml  -n ${MM_NAMESPACE}"

    SECRETKEY=$(openssl rand -hex 32)
    sed -i "s/<secretkey>/$SECRETKEY/g" ${RESOURCEDIR}/trustyai/sample-minio.yaml || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/sample-minio.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
    #os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/openvino-serving-runtime.yaml -n ${MM_NAMESPACE}"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/models/minio_sklearn_mlserver_model.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/trustyai_crd.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
}

function check_trustyai_resources() {
  header "Checking that TrustyAI resources have spun up"
  oc project $MM_NAMESPACE  || eval "$FAILURE_HANDLING"

  os::cmd::try_until_text "oc get deployment trustyai-service" "trustyai-service" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get route trustyai-service-route" "trustyai-service-route" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get pod | grep trustyai-service" "2/2" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"

}

function check_mm_resources() {
  header "Checking that ModelMesh resources have spun up"
  oc project $MM_NAMESPACE || eval "$FAILURE_HANDLING"

  os::cmd::try_until_text "oc get pod | grep modelmesh-serving-mlserver" "5/5" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get route example-sklearn-isvc" "example-sklearn-isvc" $odhdefaulttimeout $odhdefaultinterval
  INFER_ROUTE=$(oc get route example-sklearn-isvc --template={{.spec.host}}{{.spec.path}}) || eval "$FAILURE_HANDLING"
  token=$(oc create token user-one -n ${MM_NAMESPACE}) || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/trustyai/data.json -H 'Authorization: Bearer $token' -i" "model_name" || eval "$FAILURE_HANDLING"
}

function check_communication(){
    header "Check communication between TrustyAI and ModelMesh"
    oc project $MM_NAMESPACE || eval "$FAILURE_HANDLING"

    # send some data to modelmesh
    os::cmd::expect_success_and_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/trustyai/data.json -H 'Authorization: Bearer $token' -i" "model_name" || eval "$FAILURE_HANDLING"
    os::cmd::try_until_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/trustyai/data.json -H 'Authorization: Bearer $token' -i; oc logs $(oc get pods -o name | grep trustyai-service)" "Received partial input payload" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
}

function generate_data(){
    header "Generate some data for TrustyAI (this will take a sec)"
    oc project $MM_NAMESPACE || eval "$FAILURE_HANDLING"

    # send a bunch of random data to the model
    DIVISOR=128.498 # divide bash's $RANDOM by this to get a float range of [0.,255.], for MNIST
    for i in {1..500};
    do
      DATA=$(sed "s/\[40.83, 3.5, 0.5, 0\]/\[$(($RANDOM % 2)),$(($RANDOM / 128)),$(($RANDOM / 128)), $(($RANDOM / 128)) \]/" ${RESOURCEDIR}/trustyai/data.json) || eval "$FAILURE_HANDLING"
      curl -k https://$INFER_ROUTE/infer -d "$DATA"  -H 'Authorization: Bearer $token' -i > /dev/null 2>&1 &
      sleep .01
    done
}

function schedule_and_check_request(){
  header "Create a metric request and confirm calculation"
  oc project $MM_NAMESPACE

  TRUSTY_ROUTE=https://$(oc get route/trustyai-service --template={{.spec.host}}) || eval "$FAILURE_HANDLING"

  os::cmd::expect_success_and_text "curl_token -k --location $TRUSTY_ROUTE/metrics/spd/request \
    --header 'Content-Type: application/json' \
    --data '{
        \"modelId\": \"example-sklearn-isvc\",
        \"protectedAttribute\": \"predict-0\",
        \"favorableOutcome\": 0,
        \"outcomeName\": \"predict-0\",
        \"privilegedAttribute\": 0.0,
        \"unprivilegedAttribute\": 1.0
    }'" "requestId" || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "curl_token -k $TRUSTY_ROUTE/q/metrics" "trustyai_spd" || eval "$FAILURE_HANDLING"
  REQUESTS_CREATED=true;
}


function test_prometheus_scraping(){
    header "Ensure metrics are in Prometheus"

    SECRET=`oc get secret -n openshift-user-workload-monitoring | grep  prometheus-user-workload-token | head -n 1 | awk '{print $1 }'` || eval "$FAILURE_HANDLING"
    TOKEN=`echo $(oc get secret $SECRET -n openshift-user-workload-monitoring -o json | jq -r '.data.token') | base64 -d` || eval "$FAILURE_HANDLING"
    THANOS_QUERIER_HOST=`oc get route thanos-querier -n openshift-monitoring -o json | jq -r '.spec.host'` || eval "$FAILURE_HANDLING"
    os::cmd::try_until_text "curl -X GET -kG \"https://$THANOS_QUERIER_HOST/api/v1/query?\" --data-urlencode \"query=trustyai_spd{namespace='opendatahub-model'}\" -H 'Authorization: Bearer $TOKEN' | jq '.data.result[0].metric.protected'" "predict-0" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
}

function local_teardown_wait(){
  header "Local test suite finished, pausing before teardown for any manual cluster inspection"
  echo -n "Hit enter to begin teardown: "; read
}

function teardown_trustyai_test() {
  header "Cleaning up the TrustyAI test"

  oc project $MM_NAMESPACE || eval "$FAILURE_HANDLING"
  oc get pods >> ${ARTIFACT_DIR}/${MM_NAMESPACE}.pods.txt
  oc get events >>  ${ARTIFACT_DIR}/${MM_NAMESPACE}.events.txt
  oc logs -n opendatahub $(oc get pods -o name -n opendatahub | grep trustyai) >> ${ARTIFACT_DIR}/${ODHPROJECT}.trustyoperatorlogs.txt
  oc logs $(oc get pods -o name | grep trustyai) >> ${ARTIFACT_DIR}/trusty_service_pod_logs.txt || true
  oc exec $(oc get pods -o name | grep trustyai) -c trustyai-service -- bash -c "ls /inputs/" >> ${ARTIFACT_DIR}/trusty_service_inputs_ls.txt || true

  
  
  TRUSTY_ROUTE=http://$(oc get route/trustyai-service --template={{.spec.host}}) || eval "$FAILURE_HANDLING"

  if [ $REQUESTS_CREATED = true ]; then
    for METRIC_NAME in "spd" "dir"
    do
      curl_token -sk $TRUSTY_ROUTE/metrics/$METRIC_NAME/requests
      for REQUEST in $(curl -sk -H "Authorization: Bearer ${TOKEN}" $TRUSTY_ROUTE/metrics/$METRIC_NAME/requests | jq -r '.requests [].id')
      do
        echo -n $REQUEST": "
        curl_token -k -X DELETE --location $TRUSTY_ROUTE/metrics/$METRIC_NAME/request \
            -H 'Content-Type: application/json' \
            -d "{
                  \"requestId\": \"$REQUEST\"
                }"
        echo
      done
    done
  fi

  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/secret.yaml" || eval "$FAILURE_HANDLING"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/odh-mlserver-1.x.yaml" || eval "$FAILURE_HANDLING"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/trustyai_crd.yaml"  || eval "$FAILURE_HANDLING"
  os::cmd::expect_success "oc delete project $MM_NAMESPACE" || eval "$FAILURE_HANDLING"

  oc project $ODHPROJECT || eval "$FAILURE_HANDLING"
  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/trustyai_operator_kfdef.yaml"  || eval "$FAILURE_HANDLING"
  oc delete deployment trustyai-service-operator-controller-manager  || echo "No trustyai operator deployment found"
}

if [ $TEARDOWN = false ]; then
  setup_monitoring
  [ $FAILURE = false ] && install_trustyai_operator   || echo -e "\033[0;31mSkipping TrustyAI-Operator deployment due to previous failure\033[0m"
  [ $FAILURE = false ] && deploy_model                || echo -e "\033[0;31mSkipping model deployment due to previous failure\033[0m"
  [ $FAILURE = false ] && check_trustyai_resources    || echo -e "\033[0;31mSkipping TrustyAI resource check due to previous failure\033[0m"
  [ $FAILURE = false ] && check_mm_resources          || echo -e "\033[0;31mSkipping ModelMesh resource check due to previous failure\033[0m"
  [ $FAILURE = false ] && check_communication         || echo -e "\033[0;31mSkipping ModelMesh-TrustyAI communication check due to previous failure\033[0m"
  [ $FAILURE = false ] && generate_data               || echo -e "\033[0;31mSkipping data generation due to previous failure\033[0m"
  [ $FAILURE = false ] && schedule_and_check_request  || echo -e "\033[0;31mSkipping metric scheduling due to previous failure\033[0m"
  [ $FAILURE = false ] && test_prometheus_scraping    || echo -e "\033[0;31mSkipping Prometheus data check due to previous failure\033[0m"

  [ $LOCAL = true ] && local_teardown_wait
fi
teardown_trustyai_test

[ $FAILURE = true ] && os::cmd::expect_success "echo 'A previous assertion failed, marking suite as failed' && exit 1"

os::test::junit::declare_suite_end
