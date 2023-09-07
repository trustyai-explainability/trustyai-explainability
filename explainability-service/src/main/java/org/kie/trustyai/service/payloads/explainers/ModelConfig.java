package org.kie.trustyai.service.payloads.explainers;

/**
 * This class is used to configure the model endpoint to be used for the explanation.
 */
public class ModelConfig {
    private String target;
    private String name;
    private String version;

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
