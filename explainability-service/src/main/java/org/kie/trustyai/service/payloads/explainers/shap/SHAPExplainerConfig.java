package org.kie.trustyai.service.payloads.explainers.shap;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.local.shap.ShapConfig;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "Configuration for the LIME explainer")
@Tag(name = "Payloads", description = "Payload definitions for the API")
public class SHAPExplainerConfig {

    @Schema(required = false, description = "Number of data samples to run when computing SHAP values", defaultValue = "100", example = "100")
    @JsonProperty(value = "n_samples", required = false, defaultValue = "100")
    private int nSamples = 100;

    @Schema(required = false, description = "Either LOGIT or IDENTITY. If you want the SHAP values to sum to the exact model output, use IDENTITY" +
            "If your model outputs probabilities and you want the SHAP values to" +
            "use log-odds units, use LOGIT", defaultValue = "IDENTITY", example = "IDENTITY")
    @JsonProperty(value = "link", required = false, defaultValue = "IDENTITY")
    private ShapConfig.LinkType linkType = ShapConfig.LinkType.IDENTITY;

    @Schema(required = false, description = "The choice of regularizer to use when fitting data. This will select a certain fraction " +
            "of features to use, based on which are most important to the regression", defaultValue = "AUTO", example = "AUTO")
    @JsonProperty(value = "regularizer", required = false, defaultValue = "AUTO")
    private ShapConfig.RegularizerType regularizer = ShapConfig.RegularizerType.AUTO;

    @Schema(required = false, description = "The size of the confidence window to use for SHAP values", defaultValue = "0.95", example = "0.95")
    @JsonProperty(value = "confidence", required = false, defaultValue = "0.95")
    private double confidence = 0.95;

    @Schema(required = false, description = "Whether to track byproduct counterfactuals generated during explanation", defaultValue = "false", example = "false")
    @JsonProperty(value = "track_counterfactuals", required = false, defaultValue = "false")
    private boolean trackCounterfactuals = false;

    public int getnSamples() {
        return nSamples;
    }

    public void setnSamples(int nSamples) {
        if (nSamples <= 0) {
            throw new IllegalArgumentException("Number of samples must be > 0");
        }
        this.nSamples = nSamples;
    }

    public ShapConfig.LinkType getLinkType() {
        return linkType;
    }

    public void setLinkType(ShapConfig.LinkType linkType) {
        this.linkType = linkType;
    }

    public ShapConfig.RegularizerType getRegularizer() {
        return regularizer;
    }

    public void setRegularizer(ShapConfig.RegularizerType regularizer) {
        this.regularizer = regularizer;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        if (confidence <= 0.0) {
            throw new IllegalArgumentException("Confidence must be > 0");
        }
        this.confidence = confidence;
    }

    public boolean isTrackCounterfactuals() {
        return trackCounterfactuals;
    }

    public void setTrackCounterfactuals(boolean trackCounterfactuals) {
        this.trackCounterfactuals = trackCounterfactuals;
    }
}
