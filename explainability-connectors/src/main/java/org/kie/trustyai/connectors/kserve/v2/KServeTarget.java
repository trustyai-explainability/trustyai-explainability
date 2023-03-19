package org.kie.trustyai.connectors.kserve.v2;

public class KServeTarget {

    private static final CodecParameter DEFAULT_CODEC = CodecParameter.NUMPY;
    private String target;
    private String modelId;
    private String version;
    private CodecParameter codec;

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

    public void setTarget(String target) {
        this.target = target;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public CodecParameter getCodec() {
        return codec;
    }
}
