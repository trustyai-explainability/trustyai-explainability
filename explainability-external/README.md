# TrustyAI external algorithms

The `trustyai-external` package contains the tools to extend the TrustyAI service using external algorithms not written
in Java.

At the moment, the only supported language is Python.

## Example

The `trustyaiexternal` module (found in the [trustyaiexternal](src/main/python/trustyaiexternal) directory) contains the
framework to implement your own Python algorithm.

In this guide we will implement a generic Python explainer called `MyPythonExplainer` that will be used by the TrustyAI
service.

## Python implementation

To implement the Python explainer, we need to create a class (`MyPythonExplainer`) that extends
the `trustyaiexternal.api.explainers.Explainer` class and
implements the general logic.

The `Explainer` class requires the following parameters:

- `model_name`: the name of the model to explain
- `model_version`: the version of the model to explain
- `target`: the address of the model to use for explanations

These values are mandatory and are used by the TrustyAI service to perform inferences on a remote model behind the
scenes.

All other parameters are specific to the explainer and are passed to the constructor of the explainer.
For instance, we could define our explainer as follows, under the
package `trustyaiexternal/algorithms/mypythonexplainer.py`:

```python
from trustyaiexternal.api.explainers import Explainer


class MyPythonExplainer(Explainer):
    """Python entry point or MyPythonExplainer"""

    def __init__(self, model_name: str,
                 model_version: str,
                 target: str = '0.0.0.0:8080',
                 param1,  # specific parameters
                 param2,
                 ):
        super().__init__(model_name=model_name, model_version=model_version, target=target)
        self.param1 = param1
        self.param2 = param2

    def explain(self, point: pd.DataFrame) -> dict:
        """Explains a single prediction.

        Args:
            point: A single prediction.
        """
        explainer = ...  # the concrete explainer code 
        return explainer.explain(point, param1, param2)
```

## Java implementation

We now need to create the Java counterpart. On the Maven module `explainability-external`, we need to create a new
class that can be called `org.kie.trustyai.external.explainers.external.MyPythonExplainer`.

This class would need to extends the `ExternalPythonExplainer<T>` abstract class and implement the
relevant explainer interface (e.g. `LocalExplainer`, `GlobalExplainer` or `TimeSeriesExplainer`).

A basic implementation would be:

```java
package org.kie.trustyai.external.explainers.external;

public class MyPythonExplainer extends ExternalPythonExplainer<Map<String, Object>> implements LocalExplainer<MyPythonExplanation> {

    private final String NAMESPACE = "trustyaiexternal.algorithms.mypythonexplainer";
    private final String NAME = "MyPythonExplainer";


    public TSLimeExplainer(int param1, double param2,
                           String modelId, String modelVersion, String endpoint) {
        super();
        addConstructionArg("param1", param1);
        addConstructionArg("param2", param2);
        addConstructionArg("model_name", modelId);
        addConstructionArg("model_version", modelVersion);
        addConstructionArg("target", endpoint);
    }

    public CompletableFuture<MyPythonExplanation> explainAsync(Prediction prediction, PredictionProvider model, Consumer<TSLimeExplanation> intermediateResultsConsumer) {

        final Map<String, Object> args = Map.of("point", prediction);
        final Map<String, Object> result;
        try {
            result = this.invoke(args, interpreter);
        } catch (Throwable e) {
            LOG.error("Error while invoking MyPythonExplanation", e);
            throw new RuntimeException(e);
        }
        return CompletableFuture.completedFuture(new MyPythonExplanation(result));
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getName() {
        return NAME;
    }

}
```

You can see the following actions:

- The parameters needed for the Python explainer are stored in `addConstructionArg`. These are they passed to the Python
  explainer when it is instantiated.
- The arguments for the Python explainer are store in a `Map` where keys are the Python argument name and values are the
  actual value.
- The `ExternalPythonExplainer` provides a `invoke` method that takes the arguments and the interpreter and returns the
  result of the Python explainer.
- The Python function to use is identified by `getNamespace` and `getName`. In this case, the namespace
  is `trustyaiexternal.algorithms.mypythonexplainer` and the Python class is
  `MyPythonExplainer`.

## Service endpoint

After the Python and Java implementations are ready, we need to expose the Python explainer as a service endpoint.
This can be done as with any other TrustyAI service endpoint with a difference.

Python sub-interpreters are not thread-safe, so we need to make sure that the Python interpreter is not shared between
between threads.

A typical usage pattern (given a `MyPythonExplainer` endpoint) would be:

```java
@POST
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response explain(MyPythonExplainerRequest request){


// Assume data is extracted from the request
final Prediction data=...;
final int param1=...;
final double param2=...;
final String modelId=...;
final String modelVersion=...;
final String endpoint=...;

        try(SubInterpreter sub=PythonRuntimeManager.INSTANCE.getSubInterpreter()){

final LocalExplainer<MyPythonExplanation> explainer=new MyPythonExplainer(
        param1,param2,modelId,modelVersion,endpoint);

// Request the explanation
final MyPythonExplanation explanation=explainer.explainAsync(data,null).get();
        return Response.ok().entity(explanation).build();
        }catch(ExecutionException e){
        LOG.error("Error while explaining",e);
        return Response.serverError().entity(e).build();
        }catch(InterruptedException e){
        LOG.error("Error while explaining",e);
        return Response.serverError().entity(e).build();
        }

        }
```