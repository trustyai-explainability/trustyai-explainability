package org.kie.trustyai.service.payloads;

import org.kie.trustyai.explainability.local.counterfactual.CounterfactualResult;

public class CounterfactualExplanationResponse extends BaseExplanationResponse {

    public CounterfactualExplanationResponse() {
        super();
    }

    public static CounterfactualExplanationResponse from(CounterfactualResult counterfactualResult) {
        return new CounterfactualExplanationResponse();
    }

    public static BaseExplanationResponse empty() {
        return null;
    }
}
