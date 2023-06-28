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
#OPENSHIFT_OAUTH_ENDPOINT="https://$(oc get route -n openshift-authentication   oauth-openshift -o json | jq -r '.spec.host')"
#MM_NAMESPACE="${ODHPROJECT}-model"
#
#MODEL_ALPHA=demo-loan-nn-onnx-alpha
#MODEL_BETA=demo-loan-nn-onnx-beta
#
#
## trackers of test successes
#REQUESTS_CREATED=false
#FAILURE=false
#FAILURE_HANDLING='FAILURE=true && echo -e "\033[0;31mERROR\033[0m"'
#
#
#ls ${MY_DIR}
#
#for script in "$MY_DIR"/*.sh; do
#  if [ "$script" = "/root/peak/operator-tests/trustyai-explainability/basictests/clean.sh" ]; then
#    :
#  else
#    source "${script}"
#    teardown_trustyai_test
#  fi
#done
