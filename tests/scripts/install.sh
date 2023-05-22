#!/bin/bash

echo "Installing kfDef from test directory"
KFDEF_FILENAME=odh-core.yaml

set -x
## Install the opendatahub-operator
pushd ~/peak
retry=5
if ! [ -z "${SKIP_OPERATOR_INSTALL}" ]; then
    ## SKIP_OPERATOR_INSTALL is used in the opendatahub-operator repo
    ## because openshift-ci will install the operator for us
    echo "Relying on odh operator installed by openshift-ci"
    ./setup.sh -t ~/peak/operatorsetup 2>&1
else
  echo "Installing operator from community marketplace"
  while [[ $retry -gt 0 ]]; do
    ./setup.sh -o ~/peak/operatorsetup 2>&1
    if [ $? -eq 0 ]; then
      retry=-1
    else
      echo "Trying restart of marketplace community operator pod"
      oc delete pod -n openshift-marketplace $(oc get pod -n openshift-marketplace -l marketplace.operatorSource=community-operators -o jsonpath="{$.items[*].metadata.name}")
      sleep 3m
    fi  
    retry=$(( retry - 1))
    sleep 1m
  done
fi

popd
## Grabbing and applying the patch in the PR we are testing
pushd ~/src/${REPO_NAME}
if [ -z "$PULL_NUMBER" ]; then
  echo "No pull number, assuming nightly run"
else
  curl -O -L https://github.com/${REPO_OWNER}/${REPO_NAME}/pull/${PULL_NUMBER}.patch
  echo "Applying followng patch:"
  cat ${PULL_NUMBER}.patch > ${ARTIFACT_DIR}/github-pr-${PULL_NUMBER}.patch
  git apply ${PULL_NUMBER}.patch
fi

popd
## Point manifests repo uri in the KFDEF to the manifests in the PR
pushd ~/kfdef
if [ -z "$PULL_NUMBER" ]; then
  echo "No pull number, not modifying ${KFDEF_FILENAME}"
else
  if [ $REPO_NAME == "trustyai-explainability" ]; then
    echo "Setting manifests in kfctl_openshift to use pull number: $PULL_NUMBER"
    sed -i "s#uri: https://github.com/trustyai-explainability/trustyai-explainability/tarball/main#uri: https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/tarball/pull/${PULL_NUMBER}/head#" ./${KFDEF_FILENAME}
    BRANCH_SHA=$(curl  https://api.github.com/repos/trustyai-explainability/trustyai-explainability/pulls/164 | jq ".head.sha")
    sed -i "s#value: \"quay.io/trustyai/trustyai-service:latest\"#value: \"quay.io/trustyai/trustyai-service-ci:${BRANCH_SHA}#"
  fi
fi

if [ -z "${OPENSHIFT_TESTUSER_NAME}" ] || [ -z "${OPENSHIFT_TESTUSER_PASS}" ]; then
  OAUTH_PATCH_TEXT="$(cat $HOME/peak/operator-tests/odh-manifests/resources/oauth-patch.htpasswd.json)"
  echo "Creating HTPASSWD OAuth provider"
  oc apply -f $HOME/peak/operator-tests/odh-manifests/resources/htpasswd.secret.yaml

  # Test if any oauth identityProviders exists. If not, initialize the identityProvider list
  if ! oc get oauth cluster -o json | jq -e '.spec.identityProviders' ; then
    echo 'No oauth identityProvider exists. Initializing oauth .spec.identityProviders = []'
    oc patch oauth cluster --type json -p '[{"op": "add", "path": "/spec/identityProviders", "value": []}]'
  fi

  # Patch in the htpasswd identityProvider prevent deletion of any existing identityProviders like ldap
  #  We can have multiple identityProvdiers enabled aslong as their 'name' value is unique
  oc patch oauth cluster --type json -p '[{"op": "add", "path": "/spec/identityProviders/-", "value": '"$OAUTH_PATCH_TEXT"'}]'

  export OPENSHIFT_TESTUSER_NAME=admin
  export OPENSHIFT_TESTUSER_PASS=admin
fi


if ! [ -z "${SKIP_KFDEF_INSTALL}" ]; then
  ## SKIP_KFDEF_INSTALL is useful in an instance where the 
  ## operator install comes with an init container to handle
  ## the KfDef creation
  echo "Relying on existing KfDef because SKIP_KFDEF_INSTALL was set"
else


  oc get crd odhdashboardconfigs.opendatahub.io
  result=$?
  # Apply ODH Dashboard CRDs if not applied
  # In ODH 1.4.1, the CRDs will be bundled with the ODH operator install
  if [ "$result" -ne 0 ]; then
    echo "Deploying missing ODH Dashboard CRDs"
    oc apply -k $HOME/peak/operator-tests/odh-manifests/resources/odh-dashboard/crd
  fi

  echo "Creating the following KfDef"
  cat ./${KFDEF_FILENAME} > ${ARTIFACT_DIR}/${KFDEF_FILENAME}
  oc apply -f ./${KFDEF_FILENAME}
  kfctl_result=$?
  if [ "$kfctl_result" -ne 0 ]; then
    echo "The installation failed"
    exit $kfctl_result
  fi
fi
set +x
popd
