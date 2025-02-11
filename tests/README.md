# TrustyAI CI Testing
The home of the TrustyAI CI tests is now https://github.com/trustyai-explainability/trustyai-tests

## Running the CI locally
```bash
make test LOCAL=true
```

## Useful Arguments

### make build
* `CACHE_ARG="--no-cache"`
  * Rebuild the test docker image from scratch
* `TRUSTYAI_TESTS_ORG`, `TRUSTYAI_TESTS_REPO`
  * Which Github org and repo to clone (e.g.,  `https://github.com/${TRUSTYAI_TESTS_ORG}/${TRUSTYAI_TESTS_REPO}.git)` when grabbing the TrustyAI Python test suite, defaults to `trustyai-explainability` and `trustyai-tests` respectively.
* `TRUSTYAI_TESTS_BRANCH`
  * Which branch of the TrustyAI tests repo to use, defaults to `main`

### make run
* `SKIP_OPERATORS_INSTALLATION=true`
  * Use this argument to skip the installation of prequisite operators (ODH, Servicemesh, etc.) into your cluster. This is useful
  if they are already installed.
* `SKIP_DSC_INSTALLATION=true`
  * Use this argument to skip the installation of an ODH Data Science Cluster into your cluster. This is useful
      if you already have a DSC installed
* `LOCAL=true`
  * This flag makes the test suite stop and wait for user input between the end of a test script and cluster teardown. This prevents automatic teardown, which is useful for manual inspection of the cluster before teardown when running the tests locally.
* `TRUSTYAI_MANIFESTS_REPO=`
  * Set the URL of the TrustyAI manifests tarball. This will default to `main` if not set.
* `USE_LOCAL_OPERATOR_CONFIG=true` use whatever inside of your local copy of `custom_operator_config.yaml` instead of the upstream operators spec
from the tests repo. This is useful for testing against arbitrary ODH versions, for example. 
* `PYTEST_MARKERS=`
  * Use this to restrict which tests you are running, e.g., `PYTEST_MARKERS="openshift and pvc and modelmesh"`

All of the above arguments can also be passed to `make test`, which will pass them to their corresponding subfunctions.

## Testing Different Operator Versions
`make fetch_custom_operator_config`
will download the [latest operator config](https://raw.githubusercontent.com/trustyai-explainability/trustyai-tests/refs/heads/main/trustyai_tests/setup/operators_config.yaml) from the trustyai-tests repo and save it into this directory. It will be saved as `custom_operators_config.yaml`, and any
changes you make to this file will be used for the CI tests when the `USE_LOCAL_OPERATOR_CONFIG=true` argument is provided to `make test`. For example, you could use this to test against 
different versions of the ODH operator, without needing to make a dev branch of the trustyai-tests repo. 