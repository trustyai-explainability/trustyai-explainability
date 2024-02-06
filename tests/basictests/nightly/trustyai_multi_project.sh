#!/bin/bash

source $TEST_DIR/common

MY_DIR=$(readlink -f `dirname "${BASH_SOURCE[0]}"`)

source ${MY_DIR}/../../util
RESOURCEDIR="${MY_DIR}/../../resources"

TEST_USER=${OPENSHIFT_TESTUSER_NAME:-"admin"} #Username used to login
TEST_PASS=${OPENSHIFT_TESTUSER_PASS:-"admin"} #Password used to login

LOCAL=${LOCAL:-false}
TEARDOWN=${TEARDOWN:-false}

MM_NAMESPACE1="${ODHPROJECT}-model1"
MM_NAMESPACE2="${ODHPROJECT}-model2"
MM_NAMESPACE3="${ODHPROJECT}-model3"

# trackers of test successes
REQUESTS_CREATED=false
FAILURE=false
FAILURE_HANDLING='FAILURE=true && echo -e "\033[0;31mERROR\033[0m"'

os::test::junit::declare_suite_start "$MY_SCRIPT"

# Function to add the Authorization token to curl commands
function curl_token() {
    NAMESPACE=$1
    shift
    TOKEN=$(oc create token user-one -n ${NAMESPACE}) || eval "$FAILURE_HANDLING"
    curl -H "Authorization: Bearer ${TOKEN}" "${@}"
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
    header "Deploying model into ModelMesh, namespace=$1"
    oc new-project $1 || true

    os::cmd::expect_success "oc project ${1}" || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/modelmesh/service_account.yaml -n ${1}" || eval "$FAILURE_HANDLING"
    oc label namespace $1 "modelmesh-enabled=true" --overwrite=true || echo "Failed to apply modelmesh-enabled label."
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/secret.yaml -n ${1}" || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/odh-mlserver-1.x.yaml  -n ${1}" || eval "$FAILURE_HANDLING"
#    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/model.yaml  -n ${1}"

    SECRETKEY=$(openssl rand -hex 32)
    sed -i "s/<secretkey>/$SECRETKEY/g" ${RESOURCEDIR}/trustyai/sample-minio.yaml || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/sample-minio.yaml -n ${1}" || eval "$FAILURE_HANDLING"
    #os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/openvino-serving-runtime.yaml -n ${MM_NAMESPACE}"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/models/minio_sklearn_mlserver_model.yaml -n ${1}" || eval "$FAILURE_HANDLING"
    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/trustyai_crd.yaml -n ${1}" || eval "$FAILURE_HANDLING"
}

function check_trustyai_resources() {
  header "Checking that TrustyAI resources have spun up, namespace=$1"
  oc project $1  || eval "$FAILURE_HANDLING"

  os::cmd::try_until_text "oc get deployment trustyai-service" "trustyai-service" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get route trustyai-service-route" "trustyai-service-route" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get pod | grep trustyai-service" "2/2" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"

}

function check_mm_resources() {
  header "Checking that ModelMesh resources have spun up, namespace=$1"
  oc project $1 || eval "$FAILURE_HANDLING"

  os::cmd::try_until_text "oc get pod | grep modelmesh-serving-mlserver" "5/5" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "oc get route example-sklearn-isvc" "example-sklearn-isvc" $odhdefaulttimeout $odhdefaultinterval
  INFER_ROUTE=$(oc get route example-sklearn-isvc --template={{.spec.host}}{{.spec.path}}) || eval "$FAILURE_HANDLING"
  token=$(oc create token user-one -n ${1}) || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/trustyai/data.json -H 'Authorization: Bearer $token' -i" "model_name" || eval "$FAILURE_HANDLING"
  #os::cmd::try_until_text "oc set env $(oc get pod -o name | grep modelmesh-serving -m 1) --list | grep MM_PAYLOAD_PROCESSORS" "trustyai-service" || eval "$FAILURE_HANDLING"
}

function check_communication(){
    header "Check communication between TrustyAI and ModelMesh, namespace=$1"
    oc project $1 || eval "$FAILURE_HANDLING"

    # send some data to modelmesh
    INFER_ROUTE=$(oc get route example-sklearn-isvc --template={{.spec.host}}{{.spec.path}}) || eval "$FAILURE_HANDLING"
    token=$(oc create token user-one -n ${1}) || eval "$FAILURE_HANDLING"
    os::cmd::expect_success_and_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/trustyai/data.json -H 'Authorization: Bearer $token' -i" "model_name" || eval "$FAILURE_HANDLING"
    os::cmd::try_until_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/trustyai/data.json -H 'Authorization: Bearer $token' -i; oc logs $(oc get pods -o name | grep trustyai-service)" "Received partial input payload" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
}

function generate_data(){
    header "Generate some data for TrustyAI, namespace=$1"
    oc project $1 || eval "$FAILURE_HANDLING"
    INFER_ROUTE=$(oc get route example-sklearn-isvc --template={{.spec.host}}{{.spec.path}}) || eval "$FAILURE_HANDLING"
    token=$(oc create token user-one -n ${1}) || eval "$FAILURE_HANDLING"

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
  header "Create a metric request and confirm calculation, namespace=$1"
  oc project $1

  TRUSTY_ROUTE=https://$(oc get route/trustyai-service --template={{.spec.host}}) || eval "$FAILURE_HANDLING"

  os::cmd::expect_success_and_text "curl_token $1 -k --location $TRUSTY_ROUTE/metrics/spd/request \
    --header 'Content-Type: application/json' \
    --data '{
        \"modelId\": \"example-sklearn-isvc\",
        \"protectedAttribute\": \"predict-0\",
        \"favorableOutcome\": 0,
        \"outcomeName\": \"predict-0\",
        \"privilegedAttribute\": 0.0,
        \"unprivilegedAttribute\": 1.0
    }'" "requestId" || eval "$FAILURE_HANDLING"
  os::cmd::try_until_text "curl_token $1 -k $TRUSTY_ROUTE/q/metrics" "trustyai_spd" || eval "$FAILURE_HANDLING"
  REQUESTS_CREATED=true;
}


function test_prometheus_scraping(){
    header "Ensure metrics are in Prometheus, namespace=$1"

    SECRET=`oc get secret -n openshift-user-workload-monitoring | grep  prometheus-user-workload-token | head -n 1 | awk '{print $1 }'` || eval "$FAILURE_HANDLING"
    TOKEN=`echo $(oc get secret $SECRET -n openshift-user-workload-monitoring -o json | jq -r '.data.token') | base64 -d` || eval "$FAILURE_HANDLING"
    THANOS_QUERIER_HOST=`oc get route thanos-querier -n openshift-monitoring -o json | jq -r '.spec.host'` || eval "$FAILURE_HANDLING"
    os::cmd::try_until_text "curl -X GET -kG \"https://$THANOS_QUERIER_HOST/api/v1/query?\" --data-urlencode \"query=trustyai_spd{namespace='$1'}\" -H 'Authorization: Bearer $TOKEN' | jq '.data.result[0].metric.protected'" "predict-0" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
}

function local_teardown_wait(){
  header "Local test suite finished, pausing before teardown for any manual cluster inspection"
  echo -n "Hit enter to begin teardown: "; read
}

function teardown_trustyai_test() {
  header "Cleaning up the TrustyAI test, namespace=$1"

  oc project $1 || eval "$FAILURE_HANDLING"
  oc get pods >> ${ARTIFACT_DIR}/${1}.pods.txt
  oc get events >>  ${ARTIFACT_DIR}/${1}.events.txt

  oc project $1 || eval "$FAILURE_HANDLING"
  oc logs $(oc get pods -o name | grep trustyai) >> ${ARTIFACT_DIR}/${1}.trusty_service_pod_logs.txt || true
  oc exec $(oc get pods -o name | grep trustyai) -c trustyai-service -- bash -c "ls /inputs/" >> ${ARTIFACT_DIR}/${1}.trusty_service_inputs_ls.txt || true

  TRUSTY_ROUTE=https://$(oc get route/trustyai-service --template={{.spec.host}}) || eval "$FAILURE_HANDLING"

  if [ $REQUESTS_CREATED = true ]; then
    for METRIC_NAME in "spd" "dir"
    do
      for REQUEST in $(curl_token $1 -sk $TRUSTY_ROUTE/metrics/$METRIC_NAME/requests | jq -r '.requests [].id')
      do
        echo -n $REQUEST": "
        curl_token $1 -k -X DELETE --location $TRUSTY_ROUTE/metrics/$METRIC_NAME/request \
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
  os::cmd::expect_success "oc delete project $1" || eval "$FAILURE_HANDLING"
}

function teardown_global() {
  header "Cleaning up the TrustyAI test, ODH namespace"
  oc project $ODHPROJECT
  oc logs $(oc get pods -o name -n opendatahub | grep trustyai) >> ${ARTIFACT_DIR}/${ODHPROJECT}.trustyoperatorlogs.txt

  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/trustyai_operator_kfdef.yaml"  || eval "$FAILURE_HANDLING"
  oc delete deployment trustyai-service-operator-controller-manager  || echo "No trustyai operator deployment found"
}

if [ $TEARDOWN = false ]; then
  setup_monitoring

  [ $FAILURE = false ] && install_trustyai_operator                 || echo -e "\033[0;31mSkipping TrustyAI-Operator deployment due to previous failure\033[0m"

  # deploy models
  [ $FAILURE = false ] && deploy_model $MM_NAMESPACE1               || echo -e "\033[0;31mSkipping model deployment in namespace 1 due to previous failure\033[0m"
  [ $FAILURE = false ] && deploy_model $MM_NAMESPACE2               || echo -e "\033[0;31mSkipping model deployment in namespace 2 due to previous failure\033[0m"
  #[ $FAILURE = false ] && deploy_model $MM_NAMESPACE3               || echo -e "\033[0;31mSkipping model deployment in namespace 3 due to previous failure\033[0m"

  # install trustyai
  [ $FAILURE = false ] && check_trustyai_resources $MM_NAMESPACE1   || echo -e "\033[0;31mSkipping TrustyAI resource check in namespace 1 due to previous failure\033[0m"
  [ $FAILURE = false ] && check_trustyai_resources $MM_NAMESPACE2   || echo -e "\033[0;31mSkipping TrustyAI resource check in namespace 2 due to previous failure\033[0m"
  #[ $FAILURE = false ] && check_trustyai_resources $MM_NAMESPACE3   || echo -e "\033[0;31mSkipping TrustyAI resource check in namespace 3 due to previous failure\033[0m"

  # check on models
  [ $FAILURE = false ] && check_mm_resources $MM_NAMESPACE1         || echo -e "\033[0;31mSkipping ModelMesh resource check in namespace 1 due to previous failure\033[0m"
  [ $FAILURE = false ] && check_mm_resources $MM_NAMESPACE2         || echo -e "\033[0;31mSkipping ModelMesh resource check in namespace 2 due to previous failure\033[0m"
  #[ $FAILURE = false ] && check_mm_resources $MM_NAMESPACE3         || echo -e "\033[0;31mSkipping ModelMesh resource check in namespace 3 due to previous failure\033[0m"

  # check communication
  [ $FAILURE = false ] && check_communication $MM_NAMESPACE1        || echo -e "\033[0;31mSkipping ModelMesh-TrustyAI communication check in namespace 1 due to previous failure\033[0m"
  [ $FAILURE = false ] && check_communication $MM_NAMESPACE2        || echo -e "\033[0;31mSkipping ModelMesh-TrustyAI communication check in namespace 2 due to previous failure\033[0m"
  #[ $FAILURE = false ] && check_communication $MM_NAMESPACE3        || echo -e "\033[0;31mSkipping ModelMesh-TrustyAI communication check in namespace 3 due to previous failure\033[0m"

  # generate data
  [ $FAILURE = false ] && generate_data $MM_NAMESPACE1              || echo -e "\033[0;31mSkipping data generation in namespace 1 due to previous failure\033[0m"
  [ $FAILURE = false ] && generate_data $MM_NAMESPACE2              || echo -e "\033[0;31mSkipping data generation in namespace 2 due to previous failure\033[0m"
  #[ $FAILURE = false ] && generate_data $MM_NAMESPACE3              || echo -e "\033[0;31mSkipping data generation in namespace 3 due to previous failure\033[0m"

  # schedule requests
  [ $FAILURE = false ] && schedule_and_check_request $MM_NAMESPACE1 || echo -e "\033[0;31mSkipping metric scheduling in namespace 1 due to previous failure\033[0m"
  [ $FAILURE = false ] && schedule_and_check_request $MM_NAMESPACE2 || echo -e "\033[0;31mSkipping metric scheduling in namespace 2 due to previous failure\033[0m"
  #[ $FAILURE = false ] && schedule_and_check_request $MM_NAMESPACE3 || echo -e "\033[0;31mSkipping metric scheduling in namespace 3 due to previous failure\033[0m"

  # check prometheus
  [ $FAILURE = false ] && test_prometheus_scraping $MM_NAMESPACE1       || echo -e "\033[0;31mSkipping Prometheus data check in namespace 1 due to previous failure\033[0m"
  [ $FAILURE = false ] && test_prometheus_scraping $MM_NAMESPACE2       || echo -e "\033[0;31mSkipping Prometheus data check in namespace 2 due to previous failure\033[0m"
  #[ $FAILURE = false ] && test_prometheus_scraping $MM_NAMESPACE3       || echo -e "\033[0;31mSkipping Prometheus data check in namespace 3 due to previous failure\033[0m"


  [ $LOCAL = true ] && local_teardown_wait
fi
teardown_trustyai_test $MM_NAMESPACE1
teardown_trustyai_test $MM_NAMESPACE2
#teardown_trustyai_test $MM_NAMESPACE3
teardown_global

[ $FAILURE = true ] && os::cmd::expect_success "echo 'A previous assertion failed, marking suite as failed' && exit 1"

os::test::junit::declare_suite_end
