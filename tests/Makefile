IMAGE=trustyai-service-tests
# GitHub organization where odh-manifests (or a fork) can be cloned during the build of the test container
GIT_ORG=trustyai-explainability
GIT_BRANCH=main
# Project where ODH is deployed
ODHPROJECT=opendatahub
# Specify the repo and git ref/branch to use for cloning ods-ci repo for the automation that works when running against an ODH deployment
ODS_CI_REPO=https://github.com/red-hat-data-services/ods-ci
ODS_CI_GITREF=master
OC_CLI_URL=https://mirror.openshift.com/pub/openshift-v4/amd64/clients/ocp/4.14.33/openshift-client-linux.tar.gz
# Authentication info for the OCP test user account that can be used in the test automation
# For all tests, the expectation is that this is used for normal end-user access to the cluster
OPENSHIFT_TESTUSER_NAME=
OPENSHIFT_TESTUSER_PASS=
OPENSHIFT_TESTUSER_LOGIN_PROVIDER=
# Setting SKIP_INSTALL will let you run the tests against an ODH instance that is already setup
SKIP_INSTALL=
# Setting TESTS_REGEX will allow you to change which tests are going to be run
TESTS_REGEX=
# Location inside the container where CI system will retrieve files after a test run
ARTIFACT_DIR=/tmp/artifacts
LOCAL_ARTIFACT_DIR="${PWD}/artifacts"

BUILD_TOOL?=podman
NO_CACHE?=false
LOCAL?=false
TEARDOWN?=false
PLATFORM?=linux/amd64

SERVICE_IMAGE?=quay.io/trustyai/trustyai-service:latest
OPERATOR_IMAGE?=quay.io/trustyai/trustyai-service-operator:latest

all: test
test: build run clean

build:
	${BUILD_TOOL} build -t $(IMAGE) --build-arg ORG=$(GIT_ORG) --build-arg BRANCH=$(GIT_BRANCH) --build-arg ODS_CI_REPO=$(ODS_CI_REPO) --build-arg ODS_CI_GITREF=$(ODS_CI_GITREF) --build-arg OC_CLI_URL=$(OC_CLI_URL) --platform=$(PLATFORM) . --progress=plain
run:
	# Confirm that we have a directory for storing any screenshots from selenium tests
	mkdir -p ${LOCAL_ARTIFACT_DIR}/screenshots

	oc config view --flatten --minify > /tmp/tests-kubeconfig
	${BUILD_TOOL} run -e SKIP_INSTALL=$(SKIP_INSTALL) -e TESTS_REGEX=$(TESTS_REGEX) -e SKIP_OPERATOR_INSTALL=$(SKIP_OPERATOR_INSTALL) \
	    -e SKIP_DSC_INSTALL=$(SKIP_DSC_INSTALL) -e ODHPROJECT=$(ODHPROJECT) \
		-e OPENSHIFT_TESTUSER_NAME="$(OPENSHIFT_TESTUSER_NAME)" -e OPENSHIFT_TESTUSER_PASS="$(OPENSHIFT_TESTUSER_PASS)" -e OPENSHIFT_TESTUSER_LOGIN_PROVIDER=$(OPENSHIFT_TESTUSER_LOGIN_PROVIDER) -e ARTIFACT_DIR=$(ARTIFACT_DIR) \
		-e LOCAL=$(LOCAL) -e TEARDOWN=$(TEARDOWN) \
		-e SERVICE_IMAGE=$(SERVICE_IMAGE) -e OPERATOR_IMAGE=$(OPERATOR_IMAGE)  \
		-it -v ${LOCAL_ARTIFACT_DIR}/:$(ARTIFACT_DIR):z -v /tmp/tests-kubeconfig:/tmp/kubeconfig:z $(IMAGE)

clean:
	oc delete -n $(ODHPROJECT) dsc default-dsc || true
	oc delete project $(ODHPROJECT) || echo -e "\n\n==> If the project deletion failed, you can try to use this script to force it: https://raw.githubusercontent.com/jefferyb/useful-scripts/master/openshift/force-delete-openshift-project\n\n"
	#Clean up openshift-operators namespace
	oc get csv -n openshift-operators -o name | grep strimzi-cluster-operator | xargs oc delete -n openshift-operators || true
	oc get csv -n openshift-operators -o name | grep opendatahub-operator | xargs oc delete -n openshift-operators || true
	oc delete subscription -n openshift-operators -l peak.test.subscription=opendatahub-operator
	oc get mutatingwebhookconfiguration -o name | grep katib | grep $(ODHPROJECT) | xargs oc delete || true
	oc get validatingwebhookconfiguration -o name | grep katib | grep $(ODHPROJECT) | xargs oc delete || true
	oc delete project $(ODHPROJECT)-model || true
	oc delete identity htpasswd-provider:admin || true
	oc patch $$(oc get kfdef -o name) --type=merge -p '{"metadata": {"finalizers":null}}' || true
	echo "Deleting test projects"
	oc delete $$(oc get projects -o name | grep basictests-) || true
	oc delete $$(oc get projects -o name | grep nightly-) || true
