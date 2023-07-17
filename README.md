# TrustyAI Explainability
This repo is the main hub for TrustyAI, containing the core Java library as well as various pieces
to support the [TrustyAI Service](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-service), [TrustyAI Operator](https://github.com/trustyai-explainability/trustyai-service-operator), 
and [TrustyAI Python Library](https://github.com/trustyai-explainability/trustyai-explainability-python). 

# Overview
TrustyAI is, at its core, a Java library and service for Explainable AI (XAI). TrustyAI offers fairness metrics, explainable AI algorithms,
and various other XAI tools at a library-level as well as a Docker or Kubernetes service. 

# Directory
## [Explainability-Core](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-core)
The core TrustyAI Java library, containing fairness metrics, AI explainers, and other XAI utilities.

## [Explainability-Service](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-service)
TrustyAI-as-a-service, allowing CURL access to fairness metrics and explainability algorithms within a system like
[docker-compose](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-service#using-data-in-storage-only) or a Kubernetes cluster. 

## [Explainability-Arrow](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-arrow)
A Java library to facilitate the communication between TrustyAI-Java and TrustyAI-Python using Arrow. 

## [Explainability-Connectors](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-connectors)
A Java library to facilitate the communication between the TrustyAI-Service and [KServe](https://github.com/kserve) / [ModelMesh](https://github.com/kserve/modelmesh)
via Protobuf.

## [Explainability-IntegrationTests](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-integrationtests)
A set of integration tests for integrations within [Kogito](https://kogito.kie.org/), namely DMN, PMML, and OpenNLP models.

# Roadmap
[GitHub project page](https://github.com/orgs/trustyai-explainability/projects/10)

# For More Information
Our preprint [TrustyAI Explainability Toolkit](https://arxiv.org/abs/2104.12717)
is a great source of knowledge of what the core library can offer.

Furthermore, you can reach the dev team on:
* [ODH Community Slack](https://odh-io.slack.com/archives/C03UFCVFFEY)
* [Zulip](https://kie.zulipchat.com/#narrow/stream/232681-trusty-ai)
* or by coming to one of our [community meetings](https://github.com/trustyai-explainability/community#meetings)

## Building and Contributing

All contributions are welcome! Before you start please read the [contribution guide](CONTRIBUTING.md).
