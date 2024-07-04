package org.kie.trustyai.service.payloads.explainers.shap;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SHAPExplainerConfig {
    @JsonProperty(value = "n_samples")
    private int nSamples = 100;

    public int getnSamples() {
        return nSamples;
    }

    public void setnSamples(int nSamples) {
        this.nSamples = nSamples;
    }

}
