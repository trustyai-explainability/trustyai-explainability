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
# Pytest markers to select the tests that will be executed
PYTEST_MARKERS=
# Location inside the container where CI system will retrieve files after a test run
ARTIFACT_DIR=/tmp/artifacts
LOCAL_ARTIFACT_DIR="${PWD}/artifacts"

SKIP_OPERATORS_INSTALLATION?=false
SKIP_DSC_INSTALLATION?=false
USE_LOCAL_OPERATOR_CONFIG?=false

BUILD_TOOL?=podman
CACHE_ARG=
LOCAL?=false
TEARDOWN?=false
PLATFORM?=linux/amd64
TRUSTYAI_MANIFESTS_REPO=

TRUSTYAI_TESTS_ORG?=$(GIT_ORG)
TRUSTYAI_TESTS_REPO?=trustyai-tests
TRUSTYAI_TESTS_BRANCH=$(GIT_BRANCH)

all: test
test: build run clean

build:
	${BUILD_TOOL} build -t $(IMAGE) \
 		--build-arg ODS_CI_REPO=$(ODS_CI_REPO) \
 		--build-arg ODS_CI_GITREF=$(ODS_CI_GITREF) \
 		--build-arg OC_CLI_URL=$(OC_CLI_URL) \
 		--build-arg TRUSTYAI_TESTS_ORG=$(TRUSTYAI_TESTS_ORG) \
 		--build-arg TRUSTYAI_TESTS_REPO=$(TRUSTYAI_TESTS_REPO) \
 		--build-arg TRUSTYAI_TESTS_BRANCH=$(TRUSTYAI_TESTS_BRANCH) \
 		--platform=$(PLATFORM) .  --progress=plain $(CACHE_ARG)
run:
	# Confirm that we have a directory for storing any screenshots from selenium tests
	mkdir -p ${LOCAL_ARTIFACT_DIR}/screenshots
	oc config view --flatten --minify > ${LOCAL_ARTIFACT_DIR}/tests-kubeconfig
	${BUILD_TOOL} run -e PYTEST_MARKERS="$(PYTEST_MARKERS)" \
		-e OPENSHIFT_TESTUSER_NAME="$(OPENSHIFT_TESTUSER_NAME)" -e OPENSHIFT_TESTUSER_PASS="$(OPENSHIFT_TESTUSER_PASS)" -e OPENSHIFT_TESTUSER_LOGIN_PROVIDER=$(OPENSHIFT_TESTUSER_LOGIN_PROVIDER) \
 		-e SKIP_DSC_INSTALLATION=$(SKIP_DSC_INSTALLATION) \
 		-e SKIP_OPERATORS_INSTALLATION=$(SKIP_OPERATORS_INSTALLATION) \
 		-e USE_LOCAL_OPERATOR_CONFIG=$(USE_LOCAL_OPERATOR_CONFIG) \
		-e ARTIFACT_DIR=$(ARTIFACT_DIR) \
		-e LOCAL=$(LOCAL) \
		-e TEARDOWN=$(TEARDOWN) \
		-e TRUSTYAI_MANIFESTS_REPO=$(TRUSTYAI_MANIFESTS_REPO) \
		--platform=$(PLATFORM) \
		-it -v ${LOCAL_ARTIFACT_DIR}/:$(ARTIFACT_DIR):z -v ${LOCAL_ARTIFACT_DIR}/tests-kubeconfig:/tmp/kubeconfig:z $(IMAGE)
.SILENT:
clean: clean_odh clean_operators clean_authorino clean_maria clean_tests clean_projects clean_misc

clean_odh:
	echo "============== Cleaning ODH Installation =============="
	oc delete -n $(ODHPROJECT) dsc default-dsc || true
	oc delete -n $(ODHPROJECT) dsci default-dsci || true
	oc delete project $(ODHPROJECT) || echo -e "\n\n==> If the project deletion failed, you can try to use this script to force it: https://raw.githubusercontent.com/jefferyb/useful-scripts/master/openshift/force-delete-openshift-project\n\n"

clean_operators:
	# Remove operator subscriptions, csvs
	for operator in opendatahub authorino maria serverless servicemesh; do \
  		operator_ns=$$(oc get subscription --all-namespaces | grep $$operator | awk '{print $$1}') ;\
  		if [ ! -z $$operator_ns ]; then \
  			echo "\n============== Deleting $$operator Operator Subscription and CSV from $$operator_ns==============" ;\
			oc delete $$(oc get subscription -n $$operator_ns -o name | grep $$operator)  -n $$operator_ns || true ;\
			oc delete $$(oc get clusterserviceversion -n $$operator_ns -o name | grep $$operator) -n $$operator_ns || true ;\
		fi \
	done

clean_misc:
	echo "\n============== Performing Cleanup =============="
	# Other Cleanup
	oc get mutatingwebhookconfiguration -o name | grep katib | grep $(ODHPROJECT) | xargs oc delete || true
	oc get mutatingwebhookconfiguration -o name | grep odh-model-controller | xargs oc delete || true
	oc get validatingwebhookconfiguration -o name | grep katib | grep $(ODHPROJECT) | xargs oc delete || true
	oc delete identity htpasswd-provider:admin || true

clean_maria:
	echo "\n============== Maria Cleanup =============="
	oc delete $$(oc get clusterrolebinding -o name | grep maria) || true
	oc delete $$(oc get rolebinding -o name | grep maria) || true
	oc delete $$(oc get clusterrole -o name | grep maria) || true
	oc delete $$(oc get mariadboperators --all-namespaces -o name) -n openshift-operators || true
	oc delete $$(oc get mariadboperators --all-namespaces -o name) -n mariadb-openshift-operators || true
	oc get mutatingwebhookconfiguration -o name | grep mariadb-operator-webhook | xargs oc delete || true
	oc get validatingwebhookconfiguration -o name | grep mariadb-operator-webhook | xargs oc delete || true


clean_authorino:
	echo "\n============== Authorino Cleanup =============="
	oc delete clusterrolebinding authorino-operator-manager || true
	oc delete $$(oc get clusterrolebinding -o name | grep authorino) || true
	oc delete $$(oc get clusterrole --all-namespaces -o name | grep authorino) || true

clean_tests:
	oc delete project test-namespace || true
	oc delete configmap model-serving-config -n opendatahub || true

clean_projects:
	echo "\n============== Deleting CI Projects =============="
	# Delete test projects"
	oc delete $$(oc get projects -o name | grep basictests-) || true
	oc delete $$(oc get projects -o name | grep nightly-) || true
	oc delete $$(oc get project -o name | grep -- "-openshift-operators") || true

fetch_custom_operator_config:
	wget -O custom_operators_config.yaml https://raw.githubusercontent.com/trustyai-explainability/trustyai-tests/refs/heads/main/trustyai_tests/setup/operators_config.yaml
