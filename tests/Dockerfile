FROM quay.io/centos/centos:stream8

ARG ORG=trustyai-explainability
ARG BRANCH=main
ARG ODS_CI_REPO=https://github.com/red-hat-data-services/ods-ci
# This git reference should always reference a stable commit from ods-ci that supports ODH
# This hash corresponds to a March 24th, 2023 commit
ARG ODS_CI_GITREF=a0d8e0a8cf58629f895a1660c5d766c3aa4ce82e
ARG OC_CLI_URL=https://mirror.openshift.com/pub/openshift-v4/amd64/clients/ocp/latest/openshift-client-linux.tar.gz

ENV HOME /root
WORKDIR /root

RUN dnf -y install https://dl.fedoraproject.org/pub/epel/epel-release-latest-8.noarch.rpm &&\
    dnf install -y jq bc git go-toolset python38 unzip chromium chromedriver && \
    dnf clean all && \
    git clone https://github.com/opendatahub-io/peak $HOME/peak && \
    cd $HOME/peak && \
    git submodule update --init

# install yq to help with parsing json
RUN curl -L https://github.com/mikefarah/yq/releases/download/v4.25.1/yq_linux_amd64 -o /usr/bin/yq &&\
    chmod +x /usr/bin/yq

RUN mkdir -p $HOME/src && \
    cd $HOME/src && \
    git clone --depth=1 --branch ${BRANCH} https://github.com/${ORG}/trustyai-explainability && \
    # Clone ods-ci repo at specified git ref for the ODH Dashboard webUI tests
    git clone --depth=1 ${ODS_CI_REPO} ods-ci && cd ods-ci && \
    git fetch origin ${ODS_CI_GITREF} && git checkout FETCH_HEAD && \
    chmod -R 777 $HOME/src

# Use a specific destination file name in case the url dow download name changes
ADD ${OC_CLI_URL} $HOME/peak/oc-cli.tar.gz
RUN tar -C /usr/local/bin -xvf $HOME/peak/oc-cli.tar.gz && \
    chmod +x /usr/local/bin/oc

COPY Pipfile Pipfile.lock $HOME/peak/

RUN pip3 install micropipenv &&\
    ln -s `which pip3` /usr/bin/pip &&\
    cd $HOME/peak &&\
    micropipenv install

# Install poetry to support the exeuction of ods-ci test framework
RUN curl -sSL https://install.python-poetry.org | python3 -
ENV PATH="${PATH}:$HOME/.local/bin"
RUN cd $HOME/src/ods-ci && poetry install
    

COPY setup/operatorsetup setup/pipelines-op-setup scripts/install.sh scripts/installandtest.sh $HOME/peak/
COPY resources $HOME/peak/operator-tests/trustyai-explainability/resources
COPY util $HOME/peak/operator-tests/trustyai-explainability
COPY setup/odh-core.yaml $HOME/kfdef/
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
