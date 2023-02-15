# Installation with ODH Operator

This folder consists of Kustomize compatible artifacts and a kfdef custom resource that can be used to install the Trusty AI Explainability Service with the ODH Operator

## Pre-requisites

1. OpenShift cluster, preferably 4.11 or higher
2. Open Data Hub Operator installed on the cluster. Refer to https://opendatahub.io/docs/getting-started/quick-installation.html for the most up-to-date installation instructions

## Installation

Once the Open Data Hub operator is running in your cluster, simply apply the kfdef in your desired namespace. In general we default to the `opendatahub` namespace.

`$ oc apply -f kfdef.yaml`

If you are in the process of testing out changes to manifests, update the repo reference in the kfdef.yaml to point to your branch.

```yaml
    - name: manifests
      uri: https://api.github.com/repos/trustyai-explainability/trustyai-explainability/tarball/main
```

would be updated to

```yaml
    - name: manifests
      uri: https://api.github.com/repos/<fork>/trustyai-explainability/tarball/<branch_in_fork>
```
