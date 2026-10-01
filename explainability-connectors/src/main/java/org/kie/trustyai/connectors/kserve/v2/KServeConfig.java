package org.kie.trustyai.connectors.kserve.v2;

/**
 * Configuration class for a remote Open Inference model server
 */
public class KServeConfig {

    public static final CodecParameter DEFAULT_CODEC = CodecParameter.NP;

    public static final int DEFAULT_MAXIMUM_CONCURRENT_REQUESTS = 5;

    private final String target;
    private final String modelId;
    private final String version;
    private final CodecParameter codec;
    private final int maximumConcurrentRequests;

    /**
     * Create a configuration class for a remote Open Inference model server
     * 
     * @param target The model's URI, typically a DNS name and port
     * @param modelId The model's ID
     * @param version The model version
     * @param codec The codec to use
     * @param maximumConcurrentRequests Maximum number of simultaneous request
     */
    private KServeConfig(String target, String modelId, String version, CodecParameter codec, int maximumConcurrentRequests) {
        this.target = target;
        this.modelId = modelId;
        this.version = version;
        this.codec = codec;
        this.maximumConcurrentRequests = maximumConcurrentRequests;
    }

    public static KServeConfig create(String target, String modelId, String version) {
        return new KServeConfig(target, modelId, version, DEFAULT_CODEC, DEFAULT_MAXIMUM_CONCURRENT_REQUESTS);
    }

    public static KServeConfig create(String target, String modelId, String version, CodecParameter codec) {
        return new KServeConfig(target, modelId, version, codec, DEFAULT_MAXIMUM_CONCURRENT_REQUESTS);
    }

    public static KServeConfig create(String target, String modelId, String version, CodecParameter codec, int maximumConcurrentRequests) {
        return new KServeConfig(target, modelId, version, codec, maximumConcurrentRequests);
    }

    public String getTarget() {
        return target;
    }

    public String getModelId() {
        return modelId;
    }

    public String getVersion() {
        return version;
    }

    public CodecParameter getCodec() {
        return codec;
    }

    public int getMaximumConcurrentRequests() {
        return maximumConcurrentRequests;
    }
}
