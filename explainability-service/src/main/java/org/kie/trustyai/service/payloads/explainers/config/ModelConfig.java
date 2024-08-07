package org.kie.trustyai.service.payloads.explainers.config;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is used to configure the model endpoint to be used for the explanation.
 */
@Schema(description = "Model configuration parameters")
public class ModelConfig {
    @Schema(required = true, description = "Model's name", example = "example-isvc")
    @JsonProperty("name")
    private String name;
    @Schema(required = false, description = "Model's version, optional", example = "v1")
    @JsonProperty("version")
    private String version = "v1";

    /**
     * Constructor to be used to instantiate the class.
     *
     * @param name The name of the model to be used for the explanation
     * @param version The version of the model to be used for the explanation
     */
    public ModelConfig(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public ModelConfig() {
        // NO OP
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
