package org.kie.trustyai.external.interfaces;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;

import jdk.jshell.spi.ExecutionControl;
import jep.python.PyCallable;
import jep.python.PyObject;

public class PassthroughPythonPredictionProvider implements PredictionProvider {

    private final PyCallable function;

    public PassthroughPythonPredictionProvider(PyCallable function) {
        this.function = function;
    }

    @Override
    public CompletableFuture<List<PredictionOutput>> predictAsync(List<PredictionInput> inputs) {
        return CompletableFuture.failedFuture(new ExecutionControl.NotImplementedException("This class is only intended for Python algorithms"));
    }

    public Object predict(PyObject object) {
        return function.call(object);
    }
}
