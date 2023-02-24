package org.kie.trustyai.service;

import org.kie.trustyai.service.payloads.consumer.InferencePayload;

public class PayloadProducer {

    private final static String[] encondedInputPayloads = new String[] {
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAAA6QAAAAAAAAAhAAAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAAA0QAAAAAAAAABAAAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAIBAQAAAAAAAAAAAAAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAIBKQAAAAAAAAAAAAAAAAAAA8D8=",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKi0KBWlucHV0EgRGUDY0GgIBAyoaOhgAAAAAAABLQAAAAAAAAAhAAAAAAAAA8D8="
    };

    private final static String[] encondedOutputPayloads = new String[] {
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAAAAAA==",
            "CgdleGFtcGxlGg1teSByZXF1ZXN0IGlkKh0KBWlucHV0EgRGUDY0GgIBASoKOggAAAAAAADwPw==",
    };

    public static InferencePayload getInferencePayload(int number) {
        InferencePayload payload = new InferencePayload();
        payload.setInput(encondedInputPayloads[number]);
        payload.setOutput(encondedOutputPayloads[number]);
        return payload;
    }

}
