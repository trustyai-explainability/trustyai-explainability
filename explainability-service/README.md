# trustyai-service

<!-- TOC -->

* [trustyai-service](#trustyai-service)
* [Introduction](#Introduction)
* [Running](#running)
    * [Locally](#locally)
        * [Using data in storage only](#using-data-in-storage-only)
        * [Consuming KServe v2 data](#consuming-kserve-v2-data)
* [Endpoints](#endpoints)
    * [Metrics](#metrics)
        * [Statistical Parity Difference](#statistical-parity-difference)
        * [Disparate Impact Ratio](#disparate-impact-ratio)
        * [Scheduled metrics](#scheduled-metrics)
    * [Metric Definitions](#metric-definitions)
    * [Prometheus](#prometheus)
    * [Health checks](#health-checks)
    * [Consuming KServe v2 payloads](#consuming-kserve-v2-payloads)
    * [Service info](#service-info)
* [Data sources](#data-sources)
    * [Metrics](#metrics-1)
* [Deployment](#deployment)
    * [OpenShift](#openshift)

<!-- TOC -->

# Introduction

The TrustyAI service is a REST service that provides the integration between XAI and metrics algorithms
provided by the [TrustyAI core](https://github.com/trustyai-explainability/trustyai-explainability/tree/main/explainability-core)
and external models.

Although it can be used as a standalone service, it is designed to be used as part of
[Open Data Hub](https://github.com/opendatahub-io), deployed in OpenShift/Kubernetes and managed by the [TrustyAI operator](https://github.com/trustyai-explainability/trustyai-service-operator).

The main functionality of the service is to provide a REST API to calculate metrics and explainability.
The service can consume model data (inputs and outputs) for metrics calculation either via a consumer endpoint,
or cloud events.

To provide explanations, the service performs inferences to a ModelMesh/KServe-deployed model
using gRPC.

# Running

## Locally

The TrustyAI service includes several demos.

- generating data into storage, which can be monitored by the service
- or, having a process simulating sending KServe gRPC data to a consumer endpoint

With either of these demos, the TrustyAI service will monitor the payloads and produce fairness metrics.

The first step to run the demos locally, is to build the TrustyAI service container image.
This can be done by running (on `$PROJECT/explainability-service`):

```shell
mvn clean install -Dquarkus.container-image.build=true
```

### Using data in storage only

To run this demo, first build the remaining images using:

```shell
cd demo
docker compose -f compose-partial-memory-multi-model.yaml build
```

Finally, run the demo using:

```shell
docker compose -f compose-partial-memory-multi-model.yaml up
```

Issue a metric request to, for instance:

```shell
curl -X POST --location "http://localhost:8080/metrics/spd/request" \
    -H "Content-Type: application/json" \
    -d "{
            \"modelId\": \"example-model-1\",
            \"requestName\": \"lala\",
            \"protectedAttribute\": \"inputs-2\",
            \"favorableOutcome\":  1.0,
            \"outcomeName\": \"outputs-0\",
            \"privilegedAttribute\": 1.0,
            \"unprivilegedAttribute\": 0.0
        }"
```

And observe the `trustyai_spd` metric in Prometheus: http://localhost:9090

### Consuming KServe v2 data

Another demo includes a process with simulates sending gRPC encoded KServe v2 data to a `consumer` endpoint in the
service.
The service then parses the data and saves it into storage.

To run it, start by building the necessary images with:

```shell
cd demo
docker compose -f compose-partial-pvc-single-model.yaml build
```

This demo uses a Docker bind mount, which on the host can be created with:

```shell
mkdir -p ~/volumes/pvc/inputs
```

> Note:
>
> If you are having permission errors from the service, while saving the data to the volume, change the permissions
> with `cmhod 777 ~/volume/pvc/inputs`

The demo can then be started with:

```shell
docker compose -f compose-partial-pvc-single-model.yaml up
```

After a few seconds, you will start seeing the logs showing both the payload sent

```text
generator         | Sending data
trustyai-service  | 2023-02-18 12:22:13,572 INFO  [org.kie.tru.ser.end.con.ConsumerEndpoint] (executor-thread-1) Got payload on the consumer
trustyai-service  | 2023-02-18 12:22:13,572 INFO  [org.kie.tru.ser.end.con.ConsumerEndpoint] (executor-thread-1) [Feature{name='inputs-0', type=number, value=22.0}, Feature{name='inputs-1', type=number, value=5.0}, Feature{name='inputs-2', type=number, value=1.0}]
trustyai-service  | 2023-02-18 12:22:13,572 INFO  [org.kie.tru.ser.end.con.ConsumerEndpoint] (executor-thread-1) [Output{value=1.0, type=number, score=1.0, name='outputs-0'}]
trustyai-service  | 2023-02-18 12:22:18,001 INFO  [org.kie.tru.ser.dat.par.CSVParser] (executor-thread-1) Creating dataframe from CSV data
trustyai-service  | 2023-02-18 12:22:18,001 INFO  [org.kie.tru.ser.dat.DataSource] (executor-thread-1) Batching with 5000 rows. Passing 73 rows
```

You can also inspect the data `~/volumes/pvc/inputs` in order to see what data is being serialised.

# Endpoints

The OpenAPI schema can be displayed using

```shell
curl -X GET --location "http://localhost:8080/q/openapi"
```

## Metrics

Each of the metrics default bounds can be overridden with
the corresponding environment variable, e.g.

- `METRICS_SPD_THRESHOLD_LOWER`
- `METRICS_SPD_THRESHOLD_UPPER`
- `METRICS_DIR_THRESHOLD_LOWER`
- _etc_

### Statistical Parity Difference

Get statistical parity difference at `/metrics/spd`

```shell
curl -X POST --location "http://{{host}}/metrics/spd" \
    -H "Content-Type: application/json" \
    -d "{
          \"modelId\": \"example-model-1\",
          \"protectedAttribute\": \"inputs-2\",
          \"favorableOutcome\": 1.0,
          \"outcomeName\": \"outputs-0\",
          \"privilegedAttribute\": 1.0,
          \"unprivilegedAttribute\": 0.0
        }"
```

Returns:

```http request
HTTP/1.1 200 OK
content-length: 199
Content-Type: application/json;charset=UTF-8

{
  "type": "metric",
  "name": "SPD",
  "value": -0.2531969309462916,
  "specificDefinition":"The SPD of -0.253196 indicates that the likelihood of Group:gender=1 receiving Outcome:income=1 was -25.3196 percentage points lower than that of Group:gender=0."
  "timestamp": 1675850601910,
  "thresholds": {
    "lowerBound": -0.1,
    "upperBound": 0.1,
    "outsideBounds": true
  },
  "id": "ec435fc6-d037-493b-9efc-4931138d7656"
}
```

### Disparate Impact Ratio

```shell
curl -X POST --location "http://{{host}}/metrics/dir" \
    -H "Content-Type: application/json" \
    -d "{
          \"modelId\": \"example-model-1\",
          \"protectedAttribute\": \"inputs-2\",
          \"favorableOutcome\": 1.0,
          \"outcomeName\": \"outputs-0\",
          \"privilegedAttribute\": 1.0,
          \"unprivilegedAttribute\": 0.0
        }"
```

```http request
HTTP/1.1 200 OK
content-length: 197
Content-Type: application/json;charset=UTF-8
{
  "type": "metric",
  "name": "DIR",
  "value": 0.3333333333333333,
  "specificDefinition":"The DIR of 0.33333 indicates that the likelihood of Group:gender=1 receiving Outcome:income=1 is 0.33333 times that of Group:gender=0."
  "id": "15f87802-30ae-424b-9937-1589489d6b4b",
  "timestamp": 1675850775317,
  "thresholds": {
    "lowerBound": 0.8,
    "upperBound": 1.2,
    "outsideBounds": true
  }
}
```

### Scheduled metrics

In order to generate period measurements for a certain metric, you can send a request to
the `/metrics/$METRIC/schedule`.
Looking at the SPD example above if we wanted the metric to be calculated periodically we would request:

```shell
curl -X POST --location "http://{{host}}/metrics/spd/request" \
    -H "Content-Type: application/json" \
    -d "{
          \"modelId\": \"example-model-1\",
          \"protectedAttribute\": \"inputs-2\",
          \"favorableOutcome\": 1.0
          \"outcomeName\": \"outputs-0\",
          \"privilegedAttribute\": 1.0,
          \"unprivilegedAttribute\": 0.0
        }"
```

We would get a response with the schedule id for this specific query:

```http request
HTTP/1.1 200 OK
content-length: 78
Content-Type: application/json;charset=UTF-8

{
  "requestId": "3281c891-e2a5-4eb3-b05d-7f3831acbb56",
  "timestamp": 1676031994868
}
```

The metrics will now be pushed to Prometheus with the runtime provided `SERVICE_METRICS_SCHEDULE` configuration (
e.g. `SERVICE_METRICS_SCHEDULE=10s`)
which follows the [Quarkus syntax](https://quarkus.io/guides/scheduler-reference).

You can also specify the bias threshold deltas in the request body:

```shell
curl -X POST --location "http://{{host}}/metrics/spd/request" \
    -H "Content-Type: application/json" \
    -d "{
          \"modelId\": \"example-model-1\",
          \"thresholdDelta\": 0.05,
          \"protectedAttribute\": \"inputs-2\",
          \"favorableOutcome\": 1.0,
          \"outcomeName\": \"outputs-0\",
          \"privilegedAttribute\": 1.0,
          \"unprivilegedAttribute\": 0.0
        }"
```

This means that _this specific_ metric request will consider SPD values within +/-0.05 to be fair, and values outside
those bounds to be unfair.

You can also specify the batch size in the request body:

```shell
curl -X POST --location "http://{{host}}/metrics/spd/request" \
    -H "Content-Type: application/json" \
    -d "{
          \"modelId\": \"example-model-1\",
          \"batchSize\": 1000,
          \"protectedAttribute\": \"inputs-2\",
          \"favorableOutcome\": 1.0,
          \"outcomeName\": \"outputs-0\",
          \"privilegedAttribute\": 1.0,
          \"unprivilegedAttribute\": 0.0
        }"
```

This mean that for _this specific_ metric request the dataset used will
consist of the last 1000 records.
If the batch size is omitted, the default value is taken from the configuration variable `BATCH_SIZE` as the default.

To stop the periodic calculation you can issue an HTTP `DELETE` request to the `/metrics/$METRIC/request` endpoint, with
the id of periodic task we want to cancel in the payload.
For instance:

```shell
curl -X DELETE --location "http://{{host}}:8080/metrics/spd/request" \
    -H "Content-Type: application/json" \
    -d "{
          \"requestId\": \"3281c891-e2a5-4eb3-b05d-7f3831acbb56\"
        }"
```

To list all currently active requests for a certain metric, use `GET /metrics/{{metric}}/requests`.
For instance, to get all current scheduled SPD metrics use:

```shell
curl -X GET --location "http://{{host}}:8080/metrics/spd/requests"
```

This will return, as an example:

```shell
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
content-length: 271

{
  "requests": [
    {
      "id": "ab46d639-6567-438b-a0aa-44ee9fd423a3",
      "request": {
        "protectedAttribute": "inputs-2",
        "favorableOutcome": {
          "type": "DOUBLE",
          "value": 1.0
      },
      "outcomeName": "outputs-0",
      "privilegedAttribute": {
        "type": "DOUBLE",
        "value": 1.0
      },
      "unprivilegedAttribute": {
      "type": "DOUBLE",
      "value": 0.0
      },
      "modelId": null
    }
  }
]
}
```

## Metric Definitions

To get a _general_ definition of a metric, you can issue an HTTP `GET` request to the `/metrics/$METRIC/definition`
endpoint:

```shell
curl -X GET http://{{host}}:8080/metrics/{{metric}}/definition
```

returns

```
Statistical Parity Difference (SPD) measures imbalances in classifications by calculating the difference between the proportion of the majority and protected classes getting a particular outcome. Typically, -0.1 < SPD < 0.1 indicates a fair model, while a value outside those bounds indicates an unfair model for the groups and outcomes in question"
```

To get a _specific_ definition of what a particular value means in the context of a specific computed metric,
you can issue an HTTP `POST` request to the `/metrics/$METRIC/definition` endpoint. The body of this request will
look identical to a normal metric request, except you will specify the metric value of interest within the `metricValue`
field.
This is equivalent to asking "If I computed this metric in this configuration, what would a value of $x mean?":

```shell
curl -X POST --location "http://{{host}}:8080/metrics/{metric}/definition" \
    -H "Content-Type: application/json" \
    -d "{
          \"protectedAttribute\": \"gender\",
          \"favorableOutcome\": 1
          \"outcomeName\": \"income\",
          \"privilegedAttribute\": 1
          \"unprivilegedAttribute\": 0
          \"metricValue\": 0.25
        }"
```

returns

```
The SPD of 0.250000 indicates that the likelihood of Group:gender=1 receiving Outcome:income=1 was 25.000000 percentage points higher than that of Group:gender=0.%
```

## Explainers

The TrustyAI service provides local and globalsexplainers. The supported explainers are:

- LIME
- SHAP
- Counterfactuals
- TSSaliency
- PDP

### TSSaliency

Assuming there's a ModelMesh-deployed model at host `$MODELSERVER` and using a standard `8081` port, with
name `$MODELNAME` and
version `$MODELVERSION`, a TSSaliency explainer can be invoked with:

```shell
curl -X POST --location "http://{{host}}:8080/explainers/local/tssaliency" \
    -H "Content-Type: application/json" \
    -d "{
          \"model\": {
            \"target\": "$MODELHOST:8081",
            \"name\": $MODELNAME,
            \"version\": $MODELVERSION
          },
          \"parameters\": {
            \"numberSamples\": 100,
            \"numberSteps\": 50,
            \"sigma\": 20.0,
            \"mu\": 0.1
          },
          \"data\": {
            \"f1\": [
              -0.14040239,
              0.17164128,
              0.30204415,
              0.23280369,
              0.033852769,
              -0.22418335,
              -0.46998698,
              -0.64539614,
              -0.61769196
            ]
          }
      }"
```

Where `f1` is the time-series instance to be explained.
The `parameters` section is optional and can be omitted, in which case the default values will be used.

## Prometheus

Whenever a metric endpoint is called with a HTTP request, the service also updates
the corresponding Prometheus metric.

The metrics are published at `/q/metrics` and can be consumed directly with Prometheus.
The examples also include a Grafana dashboard to visualize them.

![](docs/grafana.jpg)

Each Prometheus metric is scoped to a specific `model` and attributes using tags.
For instance, for the SPD metric request above we would have a metric:

```
trustyai_spd{
    favorable_value="1", 
    instance="trustyai:8080", 
    job="trustyai-service", 
    model="example", 
    outcome="income", 
    privileged="1", 
    protected="gender", 
    request="e4bf1430-cc33-48a0-97ce-4d0c8b2c91f0", 
    unprivileged="0"
}
```

## Health checks

The service provides an health check endpoint at `/q/health`:

```shell
curl {{host}}:8080/q/health
```

## Consuming KServe v2 payloads

The TrustyAI service provides an endpoint to consume KServe v2 inference payloads.
When received, these will be persisted to the configured storage and used,
for instance, in the calculation of metrics.

The payload consists of a JSON object with an `input` and `output` fields, which
contain the Base64 encoded raw bytes of the gRPC Protocol payload. As an example:

```shell
curl -X POST --location "http://{{host}}:8080/consumer/kserve/v2" \
-H "Content-Type: application/json" \
-d "{
  \"modelId\": \"example-2\",
  \"input\": \"CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKiUKBWlucHV0EgRGUDY0GgIBAyoSOhAAAAAAAABUQAAAAAAAABBA\",
  \"output\": \"CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==\"
}"
```

## Service info

To retrieve service info, you can issue a `GET /info`. This is will return currently registered scheduled metrics,
number of
observations in the dataset and dataset schema.

```shell
curl -X GET --location "http://{{host}}:8080/info"
```

Will return, for instance

```text
[
    {
        "metrics": {
            "scheduledMetadata": {
                "spd": 1,
                "dir": 0
            }
        },
        "data": {
            "inputSchema": {
                "items": {
                    "inputs-0": {
                        "type": "DOUBLE",
                        "name": "input-0",
                        "index": 1
                    }, 
                },
                nameMapping: {
                    "inputs-0": "Age",
                }
            },
            "outputSchema": {
                "items": {
                    "outputs-0": {
                        "type": "INT32",
                        "name": "output-0",
                        "index": 5
                    }
                },
                "nameMapping:" {
                    "outputs-0": "Income",
                }
            },
            "observations": 105,
            "modelId": "example-model-1"
        }
    },
    {
        "metrics": {
            "scheduledMetadata": {
                "spd": 1,
                "dir": 0
            }
        },
        "data": {
            "inputSchema": {
                "items": {
                    "inputs-0": {
                        "type": "DOUBLE",
                        "name": "inputs-0",
                        "index": 1
                    },
                     "inputs-1": {
                        "type": "DOUBLE",
                        "name": "inputs-1",
                        "index": 2
                    }              
                },
                nameMapping: {
                    "inputs-0": "Age",
                    "inputs-2": "Race",
                }
            },
            "outputSchema": {
                "items": {
                    "outputs-0": {
                        "type": "INT32",
                        "name": "outputs-0",
                        "index": 5
                    }
                },
                "nameMapping:" {
                    "outputs-0": "Income",
                }
            },
            "observations": 105,
            "modelId": "example-model-2"
        }
    },
    {
        "metrics": {
            "scheduledMetadata": {
                "spd": 1,
                "dir": 0
            }
        },
        "data": {
            "inputSchema": {
                "items": {
                    "inputs-0": {
                        "type": "DOUBLE",
                        "name": "inputs-0",
                        "index": 1
                    },
                     "inputs-1": {
                        "type": "DOUBLE",
                        "name": "inputs-1",
                        "index": 2
                    },
                    "input-2": {
                        "type": "DOUBLE",
                        "name": "inputs-1",
                        "index": 3
                    },
                    "inputs-3": {
                        "type": "DOUBLE",
                        "name": "inputs-3",
                        "index": 4
                    }
                },
                nameMapping: {
                    "inputs-0": "Age",
                    "inputs-2": "Race",
                    "inputs-3": "Gender",
                    "inputs-4": "Employment",
                }
            },
            "outputSchema": {
                "items": {
                    "outputs-0": {
                        "type": "INT32",
                        "name": "outputs-0",
                        "index": 5
                    },
                    "outputs-1": {
                            "type": "INT32",
                            "name": "outputs-1",
                            "index": 6
                    }
                },
                "nameMapping:" {
                    "outputs-0": "Income",
                    "outputs-1": "Credit Score",
                }
            },
            "observations": 85,
            "modelId": "example-model-3"
        }
    }
]
```

### Inference ids

To get the inference ids stored in TrustyAI for a given model `$MODEL` you can use the endpoint `/info/inference/ids/${MODEL}`. For instance, assuming a model called `foo`:

```shell
curl "http://localhost:8080/info/inference/ids/foo"
```

will return a response similar to

```json
[
  {
    "id":"a3d3d4a2-93f6-4a23-aedb-051416ecf84f",
    "timestamp":"2024-06-25T09:06:28.75701201"
  },
  // ...
]
```

This will return all stored prediction ids. If it is needed to restrict the type to organic data (i.e. data not generated by TrustyAI itself), the parameter `type=organic` can be added:

```shell
curl "http://localhost:8080/info/inference/ids/foo?type=organic"
```

If no inference was yet recorded (or the model does not exist) this endpoint will return an `HTTP 400 Bad Request` code.

## Defining Feature/Output Names

```bash
curl -X POST --location "http://localhost:8080/q/info" \
    -H "Content-Type: application/json" \
    -d '{
        "modelId": "example-model-1", 
        "inputMapping": 
            {
                "inputs-0": "age", 
                "inputs-1": "race",
                "inputs-2": "gender"
            },
        "outputMapping": 
            {
                "outputs-0": "predictedIncome=high"
            }
    }'
```

# Data sources

## Metrics

Storage backend adapters implement the `Storage` interface which has the responsibility
of reading the data from a specific storage type (flat file on PVC at the moment)
and return the inputs and outputs as `ByteBuffer`.
From there, the service converts the `ByteBuffer` into a TrustyAI `Dataframe` to be used
in the metrics calculations.

The type of datasource is passed with the environment variable `SERVICE_STORAGE_FORMAT`.

The supported data sources are:

| Type                                      | Storage property |
|-------------------------------------------|------------------|
| Kubernetes Persistent Volume Claims (PVC) | `PVC`            |
| Memory                                    | `MEMORY`         |

The data can be batched into the latest `n` observations by using the configuration key
`SERVICE_BATCH_SIZE=n`. This behaves like a `n`-size tail and its optional.
If not specified, the entire dataset is used.

# Deployment

## OpenShift

To deploy in Kubernetes or OpenShift, the connection information
can be passed into the manifest using the `ConfigMap` in [here](manifests/opendatahub/base/trustyai-configmap.yaml).

The main manifest is available [here](manifests/opendatahub/default/trustyai-deployment.yaml).

The configuration variables include:

| Environment variable    | Values         | Default | Purpose                                                                   |
|-------------------------|----------------|---------|---------------------------------------------------------------------------|
| `QUARKUS_CACHE_ENABLED` | `true`/`false` | `true`  | Enables data fetching and metric calculation caching. Enabled by default. | 
