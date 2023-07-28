# TrustyAI Explainability
This repo is the main hub for TrustyAI, containing the core Java library as well as various modules
to support the [TrustyAI Service](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-service), [TrustyAI Operator](https://github.com/trustyai-explainability/trustyai-service-operator), 
and [TrustyAI Python Library](https://github.com/trustyai-explainability/trustyai-explainability-python). 

## Overview
TrustyAI is, at its core, a Java library and service for Explainable AI (XAI). TrustyAI offers fairness metrics, explainable AI algorithms,
and various other XAI tools at a library-level as well as a containerized service and Kubernetes deployment. 

## Directory

- [explainability-core](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-core), the core TrustyAI Java module, containing fairness metrics, AI explainers, and other XAI utilities.
- [explainability-service](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-service), TrustyAI-as-a-service, a REST service for fairness metrics and explainability algorithms including [ModelMesh](https://github.com/kserve/modelmesh) integration.
- [explainability-arrow](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-arrow), a Java module to facilitate the communication between TrustyAI-Java and TrustyAI-Python using Arrow. 
- [explainability-connectors](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-connectors), A Java module to interface with different black-box predictive model deployments or inference services. Includes support for [KServe](https://github.com/kserve) / [ModelMesh](https://github.com/kserve/modelmesh) via gRPC.
- [explainability-integrationtests](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-integrationtests)
A set of integration tests for integrations within [Kogito](https://kogito.kie.org/), namely DMN, PMML, and OpenNLP models.

## Roadmap

[GitHub project page](https://github.com/orgs/trustyai-explainability/projects/10)

## For More Information

Our preprint [TrustyAI Explainability Toolkit](https://arxiv.org/abs/2104.12717)
is a great source of knowledge of what the core library can offer.

Furthermore, you can reach the dev team on:
* [ODH Community Slack](https://odh-io.slack.com/archives/C03UFCVFFEY)
* [Zulip](https://kie.zulipchat.com/#narrow/stream/232681-trusty-ai)
* or by coming to one of our [community meetings](https://github.com/trustyai-explainability/community#meetings)

## Building and Contributing

All contributions are welcome! Before you start please read the [contribution guide](CONTRIBUTING.md).
