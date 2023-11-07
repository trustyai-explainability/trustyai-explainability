package org.kie.trustyai.service.data.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferParameter;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.service.payloads.data.upload.ModelInferRequestPayload;
import org.kie.trustyai.service.payloads.data.upload.ModelInferResponsePayload;
import org.kie.trustyai.service.payloads.data.upload.TensorPayload;

import com.google.protobuf.ByteString;

public class UploadUtils {
    private static final Logger LOG = Logger.getLogger(UploadUtils.class);

    private static boolean shapeReversalCheck(List<Long> shape) {
        return shape.get(0) != 1 && shape.get(shape.size() - 1) != 1;
    }

    // fill a tensor builder based on provided datatype
    private static InferTensorContents.Builder getTensorBuilder(String datatype, Object[] data) {

        InferTensorContents.Builder inferTensorContentsBuilder = InferTensorContents.newBuilder();

        switch (datatype) {
            case "BOOL":
                inferTensorContentsBuilder.addAllBoolContents(Arrays.stream(data).map(v -> (Boolean) v).collect(Collectors.toList()));
                break;
            case "INT8":
            case "INT16":
            case "INT32":
                inferTensorContentsBuilder.addAllIntContents(Arrays.stream(data).map(v -> (Integer) v).collect(Collectors.toList()));
                break;
            case "INT64":
                if (data[0] instanceof Integer) {
                    inferTensorContentsBuilder.addAllInt64Contents(Arrays.stream(data).map(v -> ((Integer) v).longValue()).collect(Collectors.toList()));
                } else {
                    inferTensorContentsBuilder.addAllInt64Contents(Arrays.stream(data).map(v -> (Long) v).collect(Collectors.toList()));
                }
                break;
            case "FP32":
                inferTensorContentsBuilder.addAllFp32Contents(Arrays.stream(data).map(v -> Float.valueOf(v.toString())).collect(Collectors.toList()));
                break;
            case "FP64":
                inferTensorContentsBuilder.addAllFp64Contents(Arrays.stream(data).map(v -> (Double) v).collect(Collectors.toList()));
                break;
            case "BYTES":
                inferTensorContentsBuilder.addAllBytesContents(Arrays.stream(data).map(v -> (ByteString) v).collect(Collectors.toList()));
                break;
            default:
                throw new IllegalArgumentException("Input datatype=" + datatype + " unsupported. Must be one of BOOL, INT8/16/32/64, FP32/64, or BYTES");
        }

        return inferTensorContentsBuilder;
    }

    // get a tensor/payload parameter map based on passed json
    private static Map<String, InferParameter> getParameterMap(Map<String, Object> jsonParameters) {
        Map<String, InferParameter> parameterMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : jsonParameters.entrySet()) {
            InferParameter.Builder inferParamBuilder = InferParameter.newBuilder();
            if (entry.getValue() instanceof String) {
                inferParamBuilder.setStringParam((String) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                inferParamBuilder.setBoolParam((Boolean) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                inferParamBuilder.setInt64Param((Integer) entry.getValue());
            } else {
                throw new IllegalArgumentException("Inference parameter " + entry.getKey() + " has value not of type String, Bool, or Int: " + entry.getValue().toString());
            }
            parameterMap.put(entry.getKey(), inferParamBuilder.build());
        }
        return parameterMap;
    }

    private static Object[][] transpose(Object[] objectList) {
        int ncols = ((ArrayList<Object>) objectList[0]).size();
        Object[][] objectVector = new Object[ncols][objectList.length];
        for (int i = 0; i < objectList.length; i++) {
            ArrayList<Object> sublist = (ArrayList<Object>) objectList[i];
            for (int j = 0; j < ncols; j++) {
                objectVector[j][i] = sublist.get(j);
            }
        }
        return objectVector;
    }

    // request =========================================================================================================
    // initialize the input tensor builder
    private static ModelInferRequest.InferInputTensor.Builder inputBuilderInitializer(TensorPayload input, String name) {
        ModelInferRequest.InferInputTensor.Builder inputBuilder = ModelInferRequest.InferInputTensor.newBuilder();

        // the shapes are parsed in different directions in the kserve parser depending on codec
        List<Long> shape = Arrays.stream(input.getShape()).mapToLong(Number::longValue).boxed().collect(Collectors.toList());
        if (shapeReversalCheck(shape)) {
            Collections.reverse(shape);
        }
        inputBuilder.addAllShape(shape);
        inputBuilder.setDatatype(input.getDatatype());
        inputBuilder.setName(name);

        if (input.getParameters() != null) {
            inputBuilder.putAllParameters(getParameterMap(input.getParameters()));
        }
        return inputBuilder;
    }

    // populate a model infer request builder
    public static void populateRequestBuilder(ModelInferRequest.Builder inferBuilder, ModelInferRequestPayload payload) {
        if (payload.getInputs().length > 1) {
            throw new IllegalArgumentException("Passed input list must be of size 1, got size=" + payload.getInputs().length);
        }
        TensorPayload input = payload.getInputs()[0];
        if (input.getData()[0] instanceof ArrayList) {
            //transpose objects
            Object[][] objectVector = transpose(input.getData());

            for (int i = 0; i < objectVector.length; i++) {
                ModelInferRequest.InferInputTensor.Builder inputBuilder = inputBuilderInitializer(input, input.getName() + "-" + i);
                inputBuilder.setContents(getTensorBuilder(input.getDatatype(), objectVector[i]));
                inferBuilder.addInputs(inputBuilder.build());
            }
        } else {
            ModelInferRequest.InferInputTensor.Builder inputBuilder = inputBuilderInitializer(input, input.getName());
            inputBuilder.setContents(getTensorBuilder(input.getDatatype(), input.getData()).build());
            inferBuilder.addInputs(inputBuilder.build());
        }

    }

    // response ========================================================================================================
    // initialize the output tensor builder
    private static ModelInferResponse.InferOutputTensor.Builder outputBuilderInitializer(TensorPayload output, String name) {
        ModelInferResponse.InferOutputTensor.Builder outputBuilder = ModelInferResponse.InferOutputTensor.newBuilder();

        // the shapes are parsed in different directions in the kserve parser depending on codec
        List<Long> shape = Arrays.stream(output.getShape()).mapToLong(Number::longValue).boxed().collect(Collectors.toList());
        if (shapeReversalCheck(shape)) {
            Collections.reverse(shape);
        }
        outputBuilder.addAllShape(shape);

        outputBuilder.setDatatype(output.getDatatype());
        outputBuilder.setName(name);

        if (output.getParameters() != null) {
            outputBuilder.putAllParameters(getParameterMap(output.getParameters()));
        }
        return outputBuilder;
    }

    // populate a model infer response builder
    public static void populateResponseBuilder(ModelInferResponse.Builder inferBuilder, ModelInferResponsePayload payload) {

        if (payload.getModelName() != null) {
            inferBuilder.setModelName(payload.getModelName());
        }

        if (payload.getModelVersion() != null) {
            inferBuilder.setModelVersion(payload.getModelVersion());
        }
        if (payload.getId() != null) {
            inferBuilder.setId(payload.getId());
        }

        if (payload.getParameters() != null) {
            inferBuilder.putAllParameters(getParameterMap(payload.getParameters()));
        }

        if (payload.getOutputs().length > 1) {
            throw new IllegalArgumentException("Passed output list must be of size 1, got size=" + payload.getOutputs().length);
        }
        TensorPayload output = payload.getOutputs()[0];
        if (output.getData()[0] instanceof ArrayList) {
            Object[][] objectVector = transpose(output.getData());

            for (int i = 0; i < objectVector.length; i++) {
                ModelInferResponse.InferOutputTensor.Builder outputBuilder = outputBuilderInitializer(output, output.getName() + "-" + i);
                outputBuilder.setContents(getTensorBuilder(output.getDatatype(), objectVector[i]));
                inferBuilder.addOutputs(outputBuilder.build());
            }
        } else {
            ModelInferResponse.InferOutputTensor.Builder outputBuilder = outputBuilderInitializer(output, output.getName());
            outputBuilder.setContents(getTensorBuilder(output.getDatatype(), output.getData()));
            inferBuilder.addOutputs(outputBuilder.build());
        }

    }
}
