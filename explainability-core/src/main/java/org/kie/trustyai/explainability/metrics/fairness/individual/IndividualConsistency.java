package org.kie.trustyai.explainability.metrics.fairness.individual;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;

import org.kie.trustyai.explainability.model.*;

public class IndividualConsistency {
    /**
     * Calculate individual fairness in terms of consistency of predictions across similar inputs.
     *
     * @param proximityFunction a function that finds the top k similar inputs, given a reference input and a list of inputs
     * @param samples a list of inputs to be tested for consistency
     * @param predictionProvider the model under inspection
     * @return the consistency measure
     * @throws ExecutionException if any error occurs during model prediction
     * @throws InterruptedException if timeout or other interruption issues occur during model prediction
     */
    public static double calculate(BiFunction<PredictionInput, List<PredictionInput>, List<PredictionInput>> proximityFunction,
            List<PredictionInput> samples, PredictionProvider predictionProvider) throws ExecutionException, InterruptedException {
        double consistency = 1;
        for (PredictionInput input : samples) {
            List<PredictionOutput> predictionOutputs = predictionProvider.predictAsync(List.of(input)).get();
            PredictionOutput predictionOutput = predictionOutputs.get(0);
            List<PredictionInput> neighbors = proximityFunction.apply(input, samples);
            List<PredictionOutput> neighborsOutputs = predictionProvider.predictAsync(neighbors).get();
            for (Output output : predictionOutput.getOutputs()) {
                Value originalValue = output.getValue();
                for (PredictionOutput neighborOutput : neighborsOutputs) {
                    Output currentOutput = neighborOutput.getByName(output.getName()).orElse(null);
                    if (currentOutput != null && !originalValue.equals(currentOutput.getValue())) {
                        consistency -= 1f / (neighbors.size() * predictionOutput.getOutputs().size() * samples.size());
                    }
                }
            }
        }
        return consistency;
    }
}
