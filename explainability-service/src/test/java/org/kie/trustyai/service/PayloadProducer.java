package org.kie.trustyai.service;

import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;

public class PayloadProducer {

    public static final String MODEL_A_ID = "example1";
    public static final String MODEL_B_ID = "example2";

    private final static String[] encondedInputPayloadsA = new String[] {
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAAA6QAAAAAAAAAhAAAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAAA0QAAAAAAAAABAAAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAIBAQAAAAAAAAAAAAAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAIBKQAAAAAAAAAAAAAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAABLQAAAAAAAAAhAAAAAAAAA8D8="
    };

    private final static String[] encondedOutputPayloadsA = new String[] {
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAADwPw==",
    };

    private final static String[] encondedInputPayloadsB = new String[] {
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhjKKN5N7hA2QAXYIruGuEVAAAAAAAAAAAA=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgBODbGgzVUQB5gY0EgkWZAAAAAAAAAAAA=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhi8O6CiPPxUQMDmPMxhdek/AAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhiJsMMq6VIgQKiA2tg0vmFAAAAAAAAAAEA=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgE+Kn9M1dUQOLCk7MetBhAAAAAAAAA8D8="
    };

    private final static String[] encondedOutputPayloadsB = new String[] {
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKhkKBm91dHB1dBIFSU5UMzIaAgECKgQSAgAB",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKhkKBm91dHB1dBIFSU5UMzIaAgECKgQSAgEA",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKhkKBm91dHB1dBIFSU5UMzIaAgECKgQSAgEA",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKhkKBm91dHB1dBIFSU5UMzIaAgECKgQSAgEB",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKhkKBm91dHB1dBIFSU5UMzIaAgECKgQSAgAA"
    };

    public static InferencePayload getInferencePayloadA(int number) {
        final InferencePayload payload = new InferencePayload();
        payload.setInput(encondedInputPayloadsA[number]);
        payload.setOutput(encondedOutputPayloadsA[number]);
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

    public static InferencePayload getInferencePayloadB(int number) {
        final InferencePayload payload = new InferencePayload();
        payload.setInput(encondedInputPayloadsB[number]);
        payload.setOutput(encondedOutputPayloadsB[number]);
        payload.setModelId(MODEL_B_ID);
        return payload;
    }

    public static InferencePartialPayload getInferencePartialPayloadInput(String id, int number) {
        final InferencePartialPayload payload = new InferencePartialPayload();
        payload.setData(encondedInputPayloadsA[number]);
        payload.setId(id);
        payload.setKind(PartialKind.request);
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

    public static InferencePartialPayload getInferencePartialPayloadOutput(String id, int number) {
        final InferencePartialPayload payload = new InferencePartialPayload();
        payload.setData(encondedOutputPayloadsA[number]);
        payload.setId(id);
        payload.setKind(PartialKind.response);
        payload.setModelId(MODEL_A_ID);
        return payload;
    }

}
