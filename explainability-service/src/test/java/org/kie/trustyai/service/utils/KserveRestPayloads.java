package org.kie.trustyai.service.utils;

import java.util.stream.IntStream;

import org.kie.trustyai.service.payloads.consumer.upload.ModelInferJointPayload;
import org.kie.trustyai.service.payloads.consumer.upload.ModelInferRequestPayload;
import org.kie.trustyai.service.payloads.consumer.upload.ModelInferResponsePayload;
import org.kie.trustyai.service.payloads.consumer.upload.TensorPayload;

import com.google.protobuf.ByteString;

public class KserveRestPayloads {
    private static Object[] generateDataRow(int nCols, String datatype) {
        switch (datatype) {
            case "BOOL":
                return IntStream.range(0, nCols).mapToObj(i -> i % 2 == 0).toArray();
            case "INT8":
            case "INT16":
            case "INT32":
                return IntStream.range(0, nCols).boxed().toArray();
            case "INT64":
                return IntStream.range(0, nCols).mapToObj(i -> Long.valueOf(Integer.toString(i))).toArray();
            case "FP32":
                return IntStream.range(0, nCols).mapToObj(Float::valueOf).toArray();
            case "FP64":
                return IntStream.range(0, nCols).mapToObj(i -> (double) i / 2).toArray();
            case "BYTES":
                return IntStream.range(0, nCols).mapToObj(i -> ByteString.copyFromUtf8(Integer.toString(i))).toArray();
            default: // error case
                return IntStream.range(0, nCols).mapToObj(i -> null).toArray();
        }
    }

    public static TensorPayload generateTensor(int nRows, int nCols, String name, String datatype) {
        TensorPayload tensorPayload = new TensorPayload();
        tensorPayload.setName(name);
        if (nCols == 1) {
            tensorPayload.setShape(new Number[] { nRows });
        } else {
            tensorPayload.setShape(new Number[] { nRows, nCols });
        }
        tensorPayload.setDatatype(datatype);

        if (nRows == 1) {
            tensorPayload.setData(generateDataRow(nCols, datatype));
        } else {
            Object[][] data = new Object[nRows][nCols];
            for (int i = 0; i < nRows; i++) {
                data[i] = generateDataRow(nCols, datatype);
            }
            tensorPayload.setData(data);
        }
        return tensorPayload;
    }

    public static ModelInferJointPayload generatePayload(int nInputRows, int nInputCols, int nOutputCols, String datatype, String dataTag) {
        ModelInferJointPayload payload = new ModelInferJointPayload();
        payload.setModelName("test-model");
        payload.setDataTag(dataTag);

        ModelInferRequestPayload requestPayload = new ModelInferRequestPayload();
        requestPayload.setId("requestID");

        ModelInferResponsePayload responsePayload = new ModelInferResponsePayload();
        responsePayload.setId("requestID");
        responsePayload.setModelName("test-model__isvc-120380123");
        responsePayload.setModelVersion("1");

        requestPayload.setInputs(new TensorPayload[] { generateTensor(nInputRows, nInputCols, "input", datatype) });
        responsePayload.setOutputs(new TensorPayload[] { generateTensor(nInputRows, nOutputCols, "output", datatype) });

        payload.setRequest(requestPayload);
        payload.setResponse(responsePayload);

        return payload;
    }
}
