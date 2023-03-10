# TrustyAI OpenDataHub End2End Test

This script tests:
1) That TrustyAI deploys onto Openshift
2) That TrustyAI can deploy into an ODH environment
3) That ModelMesh receives the correct configuration from the TrustyAI deployment
4) That ModelMesh and TrustyAI can communicate payloads
5) That TrustyAI can compute metrics on received payloads
6) That TrustyAI can send those payloads to the ODH-Model-Monitoring Prometheus 

## Setup
1) Ensure you are logged into the desired cluster: `oc login ...`
2) Install the OpenDataHub operator from OperatorHub into your cluster
3) `python3 -m pip install requests`

## Usage
1) Select a particular fork of odh-manifests, e.g. https://github.com/$(REPO_OWNER)/odh-manifests
2) Choose the branch to test `$BRANCH_NAME`
3) Run the script, pointing at that fork and branch:

`./odh-deployment-e2e $REPO_OWNER $BRANCH_NAME`


## What this does (loosely)
1) Cleans your cluster of previous e2e tests
2) Deploys a minimal ODH install (currently uses PR versions of ModelMesh-serving and ModelMesh, see [here](https://github.com/RobGeada/odh-manifests/blob/trustyai-pr-images/model-mesh/base/params.env) for details)
3) Deploys TrustyAI into the cluster, which automatically configures
   1) ModelMesh to log payloads to TrustyAI
   2) Prometheus to scrape metric data from TrustyAI
4) Deploys a ModelMesh runtime pod
5) Deploys an example sklearn model to ModelMesh
6) Sends a few data points to the model
    1) Checks that these payloads are logged to TrustyAI
7) Sends a few dozen generated payloads to TrustyAI directly
8) Requests a scheduled metric calculation over the generated payloads
9) Points to the Prometheus instance to verify that the metrics are arriving correctly

# Testing Alternate Images
To test alternate versions of ModelMesh or ModelMesh-Serving, create a new branch of ODH-Manifests, and update 
the [params.env](https://github.com/RobGeada/odh-manifests/blob/trustyai-pr-images/model-mesh/base/params.env) file to
point at the new images. Then, point the [trustyai-pr-images-manifests uri](https://github.com/trustyai-explainability/trustyai-explainability/blob/406cb7e8967e9b60be4d82eb3a250d35b17f2825/e2e_tests/odh-minimal.yaml#L48) in `e2e_tests/odh-minimal.yaml`] to your branch.