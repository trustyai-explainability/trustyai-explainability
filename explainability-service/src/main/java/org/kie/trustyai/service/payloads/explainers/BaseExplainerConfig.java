package org.kie.trustyai.service.payloads.explainers;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.kie.trustyai.explainability.local.lime.LimeConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.kie.trustyai.explainability.Config.DEFAULT_ASYNC_TIMEOUT;

/**
 * Common properties across all explainer configuration payloads
 */
public abstract class BaseExplainerConfig {
    @Schema(required = false, description = "Number of samples to be generated for the local linear model training", defaultValue = "300", example = "300")
    @JsonProperty(value = "n_samples")
    private int nSamples = LimeConfig.DEFAULT_NO_OF_SAMPLES;

    @Schema(required = false, description = "Timeout (in seconds) for the LIME explainer", defaultValue = "10", example = "10")
    @JsonProperty(value = "timeout")
    private long timeout = DEFAULT_ASYNC_TIMEOUT;

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public int getnSamples() {
        return nSamples;
    }

    public void setnSamples(int nSamples) {
        if (nSamples <= 0) {
            throw new IllegalArgumentException("Number of samples must be > 0");
        }
        this.nSamples = nSamples;
    }

}
