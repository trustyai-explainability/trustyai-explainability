package org.kie.trustyai.connectors.kserve.v2;

public class KServeTarget {

    private static final CodecParameter DEFAULT_CODEC = CodecParameter.NUMPY;
    private final String target;
    private final String modelId;
    private final String version;
    private final CodecParameter codec;

    private KServeTarget(String target, String modelId, String version, CodecParameter codec) {
        this.target = target;
        this.modelId = modelId;
        this.version = version;
        this.codec = codec;
    }

    public static KServeTarget create(String target, String modelId, String version) {
        return new KServeTarget(target, modelId, version, DEFAULT_CODEC);
    }

    public static KServeTarget create(String target, String modelId, String version, CodecParameter codec) {
        return new KServeTarget(target, modelId, version, codec);
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
