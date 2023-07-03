##!/bin/bash
#
#source $TEST_DIR/common
#
#MY_DIR=$(readlink -f `dirname "${BASH_SOURCE[0]}"`)
#
#source ${MY_DIR}/../util
#RESOURCEDIR="${MY_DIR}/../resources"
#
#TEST_USER=${OPENSHIFT_TESTUSER_NAME:-"admin"} #Username used to login to the ODH Dashboard
#TEST_PASS=${OPENSHIFT_TESTUSER_PASS:-"admin"} #Password used to login to the ODH Dashboard
#LOCAL=${LOCAL:-false}
#TEARDOWN=${TEARDOWN:-false}
#MM_NAMESPACE="${ODHPROJECT}-model"
#
## trackers of test successes
#REQUESTS_CREATED=false
#FAILURE=false
#FAILURE_HANDLING='FAILURE=true && echo -e "\033[0;31mERROR\033[0m"'
#
#
#os::test::junit::declare_suite_start "$MY_SCRIPT"
#
#
#function setup_monitoring() {
#    header "Enabling User Workload Monitoring on the cluster"
#    oc apply -f ${RESOURCEDIR}/modelmesh/enable-uwm.yaml || eval "$FAILURE_HANDLING"
#}
#
#function install_trustyai(){
#  header "Installing TrustyAI"
#  oc project $ODHPROJECT || eval "$FAILURE_HANDLING"
#
#  oc apply -f ${RESOURCEDIR}/trustyai/trustyai_service_kfdef.yaml || eval "$FAILURE_HANDLING"
#  os::cmd::try_until_text "oc get pod | grep trustyai-service" "1/1" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
#}
#
#function check_trustyai_resources() {
#  header "Checking that TrustyAI resources have spun up"
#  oc project $ODHPROJECT || eval "$FAILURE_HANDLING"
#  os::cmd::try_until_text "oc get deployment modelmesh-controller" "modelmesh-controller" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
#  os::cmd::try_until_text "oc get deployment trustyai-service" "trustyai-service" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
#  os::cmd::try_until_text "oc get route trustyai-service-route" "trustyai-service-route" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
#}
#
#function deploy_model() {
#    header "Deploying model into ModelMesh"
#    oc new-project $MM_NAMESPACE  || true
#    os::cmd::expect_success "oc project $MM_NAMESPACE" || eval "$FAILURE_HANDLING"
#    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/modelmesh/service_account.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
#    oc label namespace $MM_NAMESPACE "modelmesh-enabled=true" --overwrite=true || echo "Failed to apply modelmesh-enabled label." || eval "$FAILURE_HANDLING"
#    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/secret.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
#    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/odh-mlserver-0.x.yaml  -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
##    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/default_sklearn_model.yaml  -n ${MM_NAMESPACE}"
#
#    SECRETKEY=$(openssl rand -hex 32)
#    sed -i "s/<secretkey>/$SECRETKEY/g" ${RESOURCEDIR}/trustyai/sample-minio.yaml || eval "$FAILURE_HANDLING"
#    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/sample-minio.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
#    #os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/trustyai/openvino-serving-runtime.yaml -n ${MM_NAMESPACE}"
#    os::cmd::expect_success "oc apply -f ${RESOURCEDIR}/models/openvino-inference-service.yaml -n ${MM_NAMESPACE}" || eval "$FAILURE_HANDLING"
#    sleep 30
#
#
#}
#
#function check_mm_resources() {
#  header "Checking that ModelMesh resources have spun up"
#  oc project $MM_NAMESPACE || eval "$FAILURE_HANDLING"
#  os::cmd::try_until_text "oc get route example-sklearn-isvc" "example-sklearn-isvc" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
#  INFER_ROUTE=$(oc get route example-sklearn-isvc --template={{.spec.host}}{{.spec.path}}) || eval "$FAILURE_HANDLING"
#  token=$(oc create token user-one -n ${MM_NAMESPACE}) || eval "$FAILURE_HANDLING"
#  os::cmd::try_until_text "oc get pod | grep modelmesh-serving" "5/5" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
#  os::cmd::try_until_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/trustyai/data.json -H 'Authorization: Bearer $token' -i" "model_name" || eval "$FAILURE_HANDLING"
#}
#
#function check_communication(){
#    header "Check communication between TrustyAI and ModelMesh"
#    oc project $MM_NAMESPACE || eval "$FAILURE_HANDLING"
#
#    # send some data to modelmesh
#    os::cmd::expect_success_and_text "curl -k https://$INFER_ROUTE/infer -d @${RESOURCEDIR}/trustyai/data.json -H 'Authorization: Bearer $token' -i" "model_name" || eval "$FAILURE_HANDLING"
#    oc project ${ODHPROJECT} || eval "$FAILURE_HANDLING"
#    os::cmd::try_until_text "oc logs $(oc get pods -o name | grep trustyai-service)" "Received partial input payload" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
#}
#
#function generate_data(){
#    header "Generate some data for TrustyAI (this will take a sec)"
#    oc project $MM_NAMESPACE
#
#    # send a bunch of random data to the model
#    DIVISOR=128.498 # divide bash's $RANDOM by this to get a float range of [0.,255.], for MNIST
#    for i in {1..500};
#    do
#      DATA=$(sed "s/\[40.83, 3.5, 0.5, 0\]/\[$(($RANDOM % 2)),$(($RANDOM / 128)),$(($RANDOM / 128)), $(($RANDOM / 128)) \]/" ${RESOURCEDIR}/trustyai/data.json) || eval "$FAILURE_HANDLING"
#      curl -k https://$INFER_ROUTE/infer -d "$DATA"  -H 'Authorization: Bearer $token' -i > /dev/null 2>&1 &
#      sleep .01
#    done
#}
#
#function schedule_and_check_request(){
#  header "Create a metric request and confirm calculation"
#  oc project $ODHPROJECT || eval "$FAILURE_HANDLING"
#  TRUSTY_ROUTE=http://$(oc get route/trustyai --template={{.spec.host}}) || eval "$FAILURE_HANDLING"
#
#  os::cmd::expect_success_and_text "curl -k --location $TRUSTY_ROUTE/metrics/spd/request \
#    --header 'Content-Type: application/json' \
#    --data '{
#        \"modelId\": \"example-sklearn-isvc\",
#        \"protectedAttribute\": \"input-0\",
#        \"favorableOutcome\": 0,
#        \"outcomeName\": \"output-0\",
#        \"privilegedAttribute\": 0.0,
#        \"unprivilegedAttribute\": 1.0
#    }'" "requestId" || eval "$FAILURE_HANDLING"
#  os::cmd::try_until_text "curl -k $TRUSTY_ROUTE/q/metrics" "trustyai_spd" || eval "$FAILURE_HANDLING"
#  REQUESTS_CREATED=true
#}
#
#
#function test_prometheus_scraping(){
#    header "Ensure metrics are in Prometheus"
#
#    SECRET=`oc get secret -n openshift-user-workload-monitoring | grep  prometheus-user-workload-token | head -n 1 | awk '{print $1 }'` || eval "$FAILURE_HANDLING"
#    TOKEN=`echo $(oc get secret $SECRET -n openshift-user-workload-monitoring -o json | jq -r '.data.token') | base64 -d` || eval "$FAILURE_HANDLING"
#    THANOS_QUERIER_HOST=`oc get route thanos-querier -n openshift-monitoring -o json | jq -r '.spec.host'` || eval "$FAILURE_HANDLING"
#    os::cmd::try_until_text "curl -X GET -kG \"https://$THANOS_QUERIER_HOST/api/v1/query?\" --data-urlencode \"query=trustyai_spd{namespace='opendatahub-model'}\" -H 'Authorization: Bearer $TOKEN' | jq '.data.result[0].metric.protected'" "input-0" $odhdefaulttimeout $odhdefaultinterval || eval "$FAILURE_HANDLING"
#    os::cmd::try_until_text "curl -k --location -g --request GET 'https://'$MODEL_MONITORING_ROUTE'//api/v1/query?query=trustyai_dir' -H 'Authorization: Bearer $TESTUSER_BEARER_TOKEN' -i" "value" $odhdefaulttimeout $odhdefaultinterval  || eval "$FAILURE_HANDLING"
#}
#
#function local_teardown_wait(){
#  header "Local test suite finished, pausing before teardown for any manual cluster inspection"
#  echo -n "Hit enter to begin teardown: "; read
#}
#
#function teardown_trustyai_test() {
#  header "Cleaning up the TrustyAI test"
#  oc project $ODHPROJECT
#  TRUSTY_ROUTE=http://$(oc get route/trustyai --template={{.spec.host}}) || eval "$FAILURE_HANDLING"
#
#  # delete all requests
#  if [ $REQUESTS_CREATED = true ]; then
#    for METRIC_NAME in "spd" "dir"
#    do
#      for REQUEST in $(curl -sk $TRUSTY_ROUTE/metrics/$METRIC_NAME/requests | jq -r '.requests [].id')
#      do
#        curl -k -X DELETE --location $TRUSTY_ROUTE/metrics/$METRIC_NAME/request \
#            -H 'Content-Type: application/json' \
#            -d "{
#                  \"requestId\": \"$REQUEST\"
#                }"
#        echo
#      done
#    done
#  fi
#
#  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/trustyai_service_kfdef.yaml"  || eval "$FAILURE_HANDLING"
#  os::cmd::expect_success "oc project $MM_NAMESPACE" || echo "Could not switch to $MM_NAMESPACE"
#  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/secret.yaml"  || eval "$FAILURE_HANDLING"
#  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/trustyai/odh-mlserver-0.x.yaml"  || eval "$FAILURE_HANDLING"
#  os::cmd::expect_success "oc delete -f ${RESOURCEDIR}/models/default_sklearn_model.yaml"  || eval "$FAILURE_HANDLING"
#  os::cmd::expect_success "oc delete project $MM_NAMESPACE" || eval "$FAILURE_HANDLING"
#}
#
#if [ $TEARDOWN = false ]; then
#  setup_monitoring
#  [ $FAILURE = false ] && install_trustyai            || echo -e "\033[0;31mSkipping TrustyAI install due to previous failure\033[0m"
#  [ $FAILURE = false ] && check_trustyai_resources    || echo -e "\033[0;31mSkipping TrustyAI resource check due to previous failure\033[0m"
#  [ $FAILURE = false ] && deploy_model                || echo -e "\033[0;31mSkipping model deployment due to previous failure\033[0m"
#  [ $FAILURE = false ] && check_mm_resources          || echo -e "\033[0;31mSkipping ModelMesh resource check due to previous failure\033[0m"
#  [ $FAILURE = false ] && check_communication         || echo -e "\033[0;31mSkipping ModelMesh-TrustyAI communication check due to previous failure\033[0m"
#  [ $FAILURE = false ] && generate_data               || echo -e "\033[0;31mSkipping data generation due to previous failure\033[0m"
#  [ $FAILURE = false ] && schedule_and_check_request  || echo -e "\033[0;31mSkipping metric scheduling due to previous failure\033[0m"
#  [ $FAILURE = false ] && test_prometheus_scraping    || echo -e "\033[0;31mSkipping Prometheus data check due to previous failure\033[0m"
#  [ $LOCAL = true ] && local_teardown_wait
#fi
#teardown_trustyai_test
#
#
#os::test::junit::declare_suite_end
#
