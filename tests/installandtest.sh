#!/bin/bash

#set -x
env | sort >  ${ARTIFACT_DIR}/env.txt

# set up kubeconfig
mkdir -p ~/.kube
cp /tmp/kubeconfig ~/.kube/config 2> /dev/null || cp /var/run/secrets/ci.openshift.io/multi-stage/kubeconfig ~/.kube/config
chmod 644 ~/.kube/config

HEADER="=============="
ODHPROJECT=${ODHPROJECT:-"opendatahub"}
PYTEST_MARKERS=${PYTEST_MARKERS:-"openshift and not heavy"}

echo "$HEADER Starting CI $HEADER"
echo "OCP version info"
echo `oc version`

# Determine which tarball url to use
if [ ! -z $TRUSTYAI_MANIFESTS_REPO ]; then
    :
elif [ -z "$PULL_NUMBER" ] || [ $REPO_OWNER != "trustyai-explainability" ] || [ $REPO_NAME != "trustyai-explainability" ]; then
  # if not a pull, use latest version of service
  TRUSTYAI_MANIFESTS_REPO=https://github.com/trustyai-explainability/trustyai-service-operator/tarball/main
else
  if [ $REPO_NAME == "trustyai-explainability" ]; then
    BRANCH_SHA=$(curl https://api.github.com/repos/trustyai-explainability/trustyai-explainability/pulls/${PULL_NUMBER} | jq ".head.sha" | tr -d '"')
    TRUSTYAI_MANIFESTS_REPO=https://api.github.com/repos/trustyai-explainability/trustyai-service-operator-ci/tarball/service-${BRANCH_SHA}
  fi
fi

# if using custom operator config, overwrite the default config
if [ $USE_LOCAL_OPERATOR_CONFIG = true ]; then
    echo "Using local operator configuration file."
    cp peak/custom_operators_config.yaml peak/trustyai-tests/trustyai_tests/setup/operators_config.yaml || true
fi

# Set up cluster
INSTALL_FAILURE=false
cd peak/trustyai-tests
echo $HEADER TRUSTYAI_TESTS INFO $HEADER
echo "Most recent commit: $(git show -1 --name-only)"
poetry run python trustyai_tests/setup/setup_cluster.py \
    --trustyai_manifests_url="$TRUSTYAI_MANIFESTS_REPO" \
    $( [ $SKIP_DSC_INSTALLATION = true ] && printf %s '--skip_dsc_installation' ) \
    $( [ $SKIP_OPERATORS_INSTALLATION = true ] && printf %s '--skip_operators_installation' ) \
    --artifact_dir="${ARTIFACT_DIR}" || INSTALL_FAILURE=true
exitcode=0

# Launch Test Suite
echo
echo "$HEADER Launching Test Suite $HEADER"
if [ $INSTALL_FAILURE = false ]; then
  echo -e "Running trustyai-tests suite..."
  poetry run pytest --log-cli-level=30 --tb=short --log-file=${ARTIFACT_DIR}/pytest_debug.log --log-file-level=DEBUG -m "${PYTEST_MARKERS}" --use-modelmesh-image
else
  echo -e "Skipping tests due to ODH Operator/DSC installation failure, marking suite as failed."
  exitcode=1
fi

# Report Results
if  [ "$?" -ne 0 ]; then
    echo "The tests failed"
    exitcode=1
fi

# Pause before teardown to investigate cluster
if [ $LOCAL ]; then
    echo -n "Hit enter to finish tests: "; read
fi

# Gather artifacts
# echo
#echo "$HEADER Post-Test Actions $HEADER"
#echo "Saving the dump of the pods logs in the artifacts directory"
#oc get pods -o yaml -n ${ODHPROJECT} > ${ARTIFACT_DIR}/${ODHPROJECT}-pods-postrun.yaml
#oc get pods -o yaml -n openshift-operators > ${ARTIFACT_DIR}/openshift-operators-pods-postrun.yaml
#echo "Saving the events in the artifacts directory"
#oc get events --sort-by='{.lastTimestamp}' > ${ARTIFACT_DIR}/${ODHPROJECT}-postrun-events-postrun.txt
#echo "Saving the logs from the opendatahub-operator pod in the artifacts directory"
#oc logs -n openshift-operators $(oc get pods -n openshift-operators --field-selector=spec.serviceAccountName=opendatahub-operator-controller-manager -o jsonpath="{$.items[*].metadata.name}") > ${ARTIFACT_DIR}/opendatahub-operator-postrun.log 2> /dev/null || echo "No logs for openshift-operators/opendatahub-operator"

exit $exitcode