package org.kie.trustyai.service.payloads.explainers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.trustyai.explainability.local.counterfactual.CounterfactualResult;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionOutput;

public class CounterfactualExplanationResponse extends BaseExplanationResponse {

    private boolean valid;

    private List<PredictedOutcome> outcomes;

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<PredictedOutcome> getOutcomes() {
        return outcomes;
    }

    public void setOutcomes(List<PredictedOutcome> outcomes) {
        this.outcomes = outcomes;
    }

    public static CounterfactualExplanationResponse from(CounterfactualResult counterfactualResult) {
        List<PredictionOutput> predictedOutputs = counterfactualResult.getOutput();
        CounterfactualExplanationResponse counterfactualExplanationResponse = new CounterfactualExplanationResponse();
        counterfactualExplanationResponse.setValid(counterfactualExplanationResponse.isValid());
        List<PredictedOutcome> predictedOutcomes = new ArrayList<>();
        for (PredictionOutput predictionOutput : predictedOutputs) {
            PredictedOutcome predictedOutcome = new PredictedOutcome();
            Map<String, String> outputs = new HashMap<>();
            for (Output output : predictionOutput.getOutputs()) {
                String name = output.getName();
                String valueString = output.getValue().asString();
                outputs.put(name, valueString);
            }
            predictedOutcome.setOutputs(outputs);
            predictedOutcomes.add(predictedOutcome);
        }
        counterfactualExplanationResponse.setOutcomes(predictedOutcomes);
        return counterfactualExplanationResponse;
    }

    public static BaseExplanationResponse empty() {
        return new CounterfactualExplanationResponse();
    }

    public static class PredictedOutcome {

        private Map<String, String> outputs;

        public Map<String, String> getOutputs() {
            return outputs;
        }

        public void setOutputs(Map<String, String> outputs) {
            this.outputs = outputs;
        }
    }
}
