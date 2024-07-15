#!/bin/bash

set -x
env | sort >  ${ARTIFACT_DIR}/env.txt
mkdir -p ~/.kube
cp /tmp/kubeconfig ~/.kube/config 2> /dev/null || cp /var/run/secrets/ci.openshift.io/multi-stage/kubeconfig ~/.kube/config
chmod 644 ~/.kube/config
export KUBECONFIG=~/.kube/config
export ARTIFACT_SCREENSHOT_DIR="${ARTIFACT_DIR}/screenshots"

if [ ! -d "${ARTIFACT_SCREENSHOTS_DIR}" ]; then
  echo "Creating the screenshot artifact directory: ${ARTIFACT_SCREENSHOT_DIR}"
  mkdir -p ${ARTIFACT_SCREENSHOT_DIR}
fi
TESTS_REGEX=${TESTS_REGEX:-"basictests"}
ODHPROJECT=${ODHPROJECT:-"opendatahub"}
SERVICE_IMAGE=${SERVICE_IMAGE:-"quay.io/trustyai/trustyai-service:latest"}
OPERATOR_IMAGE=${OPERATOR_IMAGE:-"quay.io/trustyai/trustyai-service-operator:latest"}

export ODHPROJECT
export LOCAL
export TEARDOWN
export SERVICE_IMAGE
export OPERATOR_IMAGE

echo "OCP version info"
echo `oc version`

echo "== Catalog Sources =="
echo `oc get catalogsources -n openshift-marketplace`

INSTALL_FAILURE=false
if [ -z "${SKIP_INSTALL}" ]; then
    # This is needed to avoid `oc status` failing inside openshift-ci
    oc new-project ${ODHPROJECT}
    oc project ${ODHPROJECT} # in case a new project is not created
    $HOME/peak/install.sh || INSTALL_FAILURE=true
    if [ ${LOCAL:-false} = true ]; then
      echo "Sleeping for 30s to let the DSC install settle"
      sleep 30s
    else
      echo "Sleeping for 5 min to let the DSC install settle"
      sleep 5m
    fi

    # Save the list of events and pods that are running prior to the test run
    oc get events --sort-by='{.lastTimestamp}' > ${ARTIFACT_DIR}/pretest-${ODHPROJECT}.events.txt
    oc get pods -o yaml -n ${ODHPROJECT} > ${ARTIFACT_DIR}/pretest-${ODHPROJECT}.pods.yaml
    oc get pods -o yaml -n openshift-operators > ${ARTIFACT_DIR}/pretest-openshift-operators.pods.yaml
fi

success=1

if [ $INSTALL_FAILURE = false ]; then
  $HOME/peak/run.sh ${TESTS_REGEX}
else
  echo -e "Skipping tests due to ODH Operator/DSC installation failure, marking suite as failed."
  success=0
fi

if  [ "$?" -ne 0 ]; then
    echo "The tests failed"
    success=0
fi

echo "Saving the dump of the pods logs in the artifacts directory"
oc get pods -o yaml -n ${ODHPROJECT} > ${ARTIFACT_DIR}/${ODHPROJECT}.pods.yaml
oc get pods -o yaml -n openshift-operators > ${ARTIFACT_DIR}/openshift-operators.pods.yaml
echo "Saving the events in the artifacts directory"
oc get events --sort-by='{.lastTimestamp}' > ${ARTIFACT_DIR}/${ODHPROJECT}.events.txt
echo "Saving the logs from the opendatahub-operator pod in the artifacts directory"
oc logs -n openshift-operators $(oc get pods -n openshift-operators --field-selector=spec.serviceAccountName=opendatahub-operator-controller-manager -o jsonpath="{$.items[*].metadata.name}") > ${ARTIFACT_DIR}/opendatahub-operator.log 2> /dev/null || echo "No logs for openshift-operators/opendatahub-operator"

if [ "$success" -ne 1 ]; then
    exit 1
fi



## Debugging pause...uncomment below to be able to poke around the test pod post-test
# echo "Debugging pause for 3 hours"
# sleep 180m
