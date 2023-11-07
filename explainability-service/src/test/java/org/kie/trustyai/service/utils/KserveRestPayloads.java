package org.kie.trustyai.service.utils;

import java.util.stream.IntStream;

import org.kie.trustyai.service.payloads.data.upload.ModelInferJointPayload;
import org.kie.trustyai.service.payloads.data.upload.ModelInferRequestPayload;
import org.kie.trustyai.service.payloads.data.upload.ModelInferResponsePayload;
import org.kie.trustyai.service.payloads.data.upload.TensorPayload;

import com.google.protobuf.ByteString;

public class KserveRestPayloads {
    private static Object[] generateDataRow(int nCols, String datatype, int offset) {
        IntStream stream = IntStream.range(0, nCols).map(i -> i + offset);
        switch (datatype) {
            case "BOOL":
                return stream.mapToObj(i -> i % 2 == 0).toArray();
            case "INT8":
            case "INT16":
            case "INT32":
                return stream.boxed().toArray();
            case "INT64":
                return stream.mapToObj(i -> Long.valueOf(Integer.toString(i))).toArray();
            case "FP32":
                return stream.mapToObj(Float::valueOf).toArray();
            case "FP64":
                return stream.mapToObj(i -> (double) i / 2).toArray();
            case "BYTES":
                return stream.mapToObj(i -> ByteString.copyFromUtf8(Integer.toString(i))).toArray();
            default: // error case
                return stream.mapToObj(i -> null).toArray();
        }
    }

    public static TensorPayload generateTensor(int nRows, int nCols, String name, String datatype, int offset) {
        TensorPayload tensorPayload = new TensorPayload();
        tensorPayload.setName(name);
        tensorPayload.setShape(new Number[] { nRows, nCols });
        tensorPayload.setDatatype(datatype);

        if (nRows == 1) {
            tensorPayload.setData(generateDataRow(nCols, datatype, offset));
        } else {
            Object[][] data = new Object[nRows][nCols];
            for (int i = 0; i < nRows; i++) {
                data[i] = generateDataRow(nCols, datatype, offset);
            }
            tensorPayload.setData(data);
        }

        return tensorPayload;
    }

    public static ModelInferJointPayload generatePayload(int nInputRows, int nInputCols, int nOutputCols, String datatype, String dataTag) {
        return generatePayload(nInputRows, nInputCols, nOutputCols, datatype, dataTag, 0, 0);
    }

    public static ModelInferJointPayload generatePayload(int nInputRows, int nInputCols, int nOutputCols, String datatype, String dataTag, int requestOffset, int responseOffset) {
        ModelInferJointPayload payload = new ModelInferJointPayload();
        payload.setModelName("test-model");
        payload.setDataTag(dataTag);

        ModelInferRequestPayload requestPayload = new ModelInferRequestPayload();
        requestPayload.setId("requestID");

        ModelInferResponsePayload responsePayload = new ModelInferResponsePayload();
        responsePayload.setId("requestID");
        responsePayload.setModelName("test-model__isvc-120380123");
        responsePayload.setModelVersion("1");

        requestPayload.setInputs(new TensorPayload[] { generateTensor(nInputRows, nInputCols, "input", datatype, requestOffset) });
        responsePayload.setOutputs(new TensorPayload[] { generateTensor(nInputRows, nOutputCols, "output", datatype, responseOffset) });

        payload.setRequest(requestPayload);
        payload.setResponse(responsePayload);

        return payload;
    }
}
