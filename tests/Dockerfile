FROM registry.access.redhat.com/ubi8:8.10-901.1716497712

ARG ORG=trustyai-explainability
ARG BRANCH=main
ARG ODS_CI_REPO=https://github.com/red-hat-data-services/ods-ci
# This git reference should always reference a stable commit from ods-ci that supports ODH
# This hash corresponds to a March 24th, 2023 commit
ARG ODS_CI_GITREF=867a617bc224726cf98fa3354293f8e50b4f5eb5
ARG OC_CLI_URL=https://mirror.openshift.com/pub/openshift-v4/amd64/clients/ocp/latest/openshift-client-linux.tar.gz

ENV HOME /root
WORKDIR /root

RUN dnf install -y jq bc git go-toolset python3.11 python3.11-devel python3.11-pip unzip && \
    dnf clean all && \
    git clone https://github.com/opendatahub-io/peak $HOME/peak && \
    cd $HOME/peak && \
    git submodule update --init

# install yq to help with parsing json
RUN curl -L https://github.com/mikefarah/yq/releases/download/v4.25.1/yq_linux_amd64 -o /usr/bin/yq &&\
    chmod +x /usr/bin/yq

# Use a specific destination file name in case the url dow download name changes
ADD ${OC_CLI_URL} $HOME/peak/oc-cli.tar.gz
RUN tar -C /usr/local/bin -xvf $HOME/peak/oc-cli.tar.gz && \
    chmod +x /usr/local/bin/oc

COPY Pipfile Pipfile.lock $HOME/peak/


COPY setup/operatorsetup scripts/install.sh scripts/installandtest.sh $HOME/peak/
COPY resources $HOME/peak/operator-tests/trustyai-explainability/resources
COPY util $HOME/peak/operator-tests/trustyai-explainability
COPY setup/odh-core-dsc.yaml setup/odh-core-dsci.yaml $HOME/kfdef/
COPY basictests $HOME/peak/operator-tests/trustyai-explainability/basictests

RUN chmod -R 777 $HOME/kfdef && \
    mkdir -p $HOME/.kube && \
    chmod -R 777 $HOME/.kube && \
    chmod -R 777 $HOME/peak && \
    mkdir -p /peak && \
    chmod -R 777 $HOME && \
    ln -s $HOME/peak/installandtest.sh /peak/installandtest.sh

# For local testing, you can add your own kubeconfig to the image
# Note:  Do not push the image to a public repo with your kubeconfig
# ADD kubeconfig /root/.kube/config

CMD $HOME/peak/installandtest.sh
