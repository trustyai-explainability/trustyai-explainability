package org.kie.trustyai.service.payloads.explainers.tssaliency;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parameters for the TSSaliency explainer.
 */
public class TSSaliencyParameters {

    private double mu = 0.01;
    @JsonProperty("numberSamples")
    private int nSamples = 50;
    @JsonProperty("numberSteps")
    private int nSteps = 10;
    private double sigma = 10.0;

    public int getnSamples() {
        return nSamples;
    }

    public void setnSamples(int nSamples) {
        this.nSamples = nSamples;
    }

    public int getnSteps() {
        return nSteps;
    }

    public void setnSteps(int nSteps) {
        this.nSteps = nSteps;
    }

    public double getSigma() {
        return sigma;
    }

    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    public double getMu() {
        return mu;
    }
}
