package org.kie.trustyai.service.payloads.explainers.tssaliency;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Configuration for the TSSaliency explainer")
@Tag(name = "Payloads", description = "Payload definitions for the API")
public class TSSaliencyExplainerConfig {

    @Schema(required = false, description = "Step size for gradient estimation", defaultValue = "0.01", example = "0.01")
    @JsonProperty("mu")
    private double mu = 0.01;

    @Schema(required = false, description = "Number of samples for gradient estimation", defaultValue = "50", example = "50")
    @JsonProperty("n_samples")
    private int nSamples = 50;
    @Schema(required = false, description = "Number of steps in convex path", defaultValue = "50", example = "50")
    @JsonProperty("n_alpha")
    private int nAlpha = 50;
    @Schema(required = false, description = "Standard deviation", defaultValue = "50", example = "50")
    @JsonProperty("sigma")
    private double sigma = 10.0;
    @Schema(required = false, description = "Feature base values. Number of elements must be the same as number of features", nullable = true)
    @JsonProperty("base_values")
    private double[] baseValues = new double[] {};

    public int getnAlpha() {
        return nAlpha;
    }

    public void setnAlpha(int nAlpha) {
        this.nAlpha = nAlpha;
    }

    public double[] getBaseValues() {
        return baseValues;
    }

    public void setBaseValues(double[] baseValues) {
        this.baseValues = baseValues;
    }

    public int getnSamples() {
        return nSamples;
    }

    public void setnSamples(int nSamples) {
        this.nSamples = nSamples;
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
