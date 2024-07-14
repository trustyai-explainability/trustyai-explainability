package org.kie.trustyai.service.payloads.explainers.config;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is used to configure the model endpoint to be used for the explanation.
 */
@Schema(description = "Model configuration parameters")
public class ModelConfig {
    @Schema(required = true, description = "Location of the model, for instance a Kubernetes service name and optionally port", example = "modelmesh-serving:8008")
    @JsonProperty("target")
    private String target;
    @Schema(required = true, description = "Model's name", example = "example-isvc")
    @JsonProperty("name")
    private String name;
    @Schema(required = false, description = "Model's version, optional", example = "v1")
    @JsonProperty("version")
    private String version = "v1";

    /**
     * Constructor to be used to instantiate the class.
     *
     * @param target The KServe/gRPC-enabled model target in the format "host:port", e.g. "my-model.com:8080" or "0.0.0.0:8080"
     * @param name The name of the model to be used for the explanation
     * @param version The version of the model to be used for the explanation
     */
    public ModelConfig(String target, String name, String version) {
        this.target = target;
        this.name = name;
        this.version = version;
    }

    public ModelConfig() {
        // NO OP
    }

    public String getTarget() {
        return target;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}
