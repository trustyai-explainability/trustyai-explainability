package org.kie.trustyai.connectors.kserve.v2;

public class KServeConfig {

    private static final CodecParameter DEFAULT_CODEC = CodecParameter.NP;
    private final String target;
    private final String modelId;
    private final String version;
    private final CodecParameter codec;

    private KServeConfig(String target, String modelId, String version, CodecParameter codec) {
        this.target = target;
        this.modelId = modelId;
        this.version = version;
        this.codec = codec;
    }

    public static KServeConfig create(String target, String modelId, String version) {
        return new KServeConfig(target, modelId, version, DEFAULT_CODEC);
    }

    public static KServeConfig create(String target, String modelId, String version, CodecParameter codec) {
        return new KServeConfig(target, modelId, version, codec);
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
}
