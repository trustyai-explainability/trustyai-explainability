package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.connectors.kserve.v2.grpc.GRPCInferenceServiceGrpc;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.connectors.utils.ListenableFutureUtils;
import org.kie.trustyai.explainability.model.*;

import com.google.common.util.concurrent.ListenableFuture;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Wraps a KServe v2-compatible model server as a TrustyAI {@link PredictionProvider}
 */
public class KServeV2GRPCPredictionProvider implements PredictionProvider {

    private static final String DEFAULT_TENSOR_NAME = "predict";
    private static final KServeDatatype DEFAULT_DATATYPE = KServeDatatype.FP64;
    private final String modelName;
    private final ManagedChannel channel;
    private final List<String> outputNames;

    private KServeV2GRPCPredictionProvider(String target, String modelName, List<String> outputNames) {

        this.modelName = modelName;
        this.channel = ManagedChannelBuilder.forTarget(target)
                .usePlaintext()
                .build();
        this.outputNames = outputNames;

    }

    /**
     * Create a {@link KServeV2GRPCPredictionProvider} for a model with an endpoint at {@code target} and named {@code modelName}.
     * In this case, the output names will be generated as {@code output-0}, {@code output-1}, ...
     *
     * @param target The remote KServe v2 model server
     * @param modelName The model's name
     * @return A {@link PredictionProvider}
     */
    public static KServeV2GRPCPredictionProvider forTarget(String target, String modelName) {
        return KServeV2GRPCPredictionProvider.forTarget(target, modelName, null);
    }

    /**
     * Create a {@link KServeV2GRPCPredictionProvider} with an endpoint at {@code target} and named {@code modelName}.
     * In this case, the output names are specified with {@code outputNames}.
     *
     * @param target The remote KServe v2 model server
     * @param modelName The model's name
     * @param outputNames A {@link List} of output names to be used
     * @return A {@link PredictionProvider}
     */
    public static KServeV2GRPCPredictionProvider forTarget(String target, String modelName, List<String> outputNames) {
        return new KServeV2GRPCPredictionProvider(target, modelName, outputNames);
    }

    private void addFeature(InferTensorContents.Builder content, Feature feature) {
        final Object object = feature.getValue().getUnderlyingObject();
        final Type type = feature.getType();

        switch (type) {
            case NUMBER:
                if (object instanceof Integer) {
                    content.addIntContents((Integer) object);
                } else if (object instanceof Double) {
                    content.addFp64Contents((Double) object);
                }
                break;
            case BOOLEAN:
                content.addBoolContents((Boolean) object);
                break;
            default:
                throw new IllegalArgumentException("Unsupported feature type: " + type);
        }
    }

    private InferTensorContents.Builder buildTensorContents(List<PredictionInput> inputs) {
        final InferTensorContents.Builder contents = InferTensorContents.newBuilder();

        inputs.stream()
                .map(PredictionInput::getFeatures)
                .forEach(features -> features.forEach(feature -> addFeature(contents, feature)));

        return contents;
    }

    private ModelInferRequest.InferInputTensor.Builder buildTensor(InferTensorContents.Builder contents, int nSamples, int nFeatures) {

        final ModelInferRequest.InferInputTensor.Builder tensor = ModelInferRequest.InferInputTensor
                .newBuilder();
        tensor.setName(DEFAULT_TENSOR_NAME)
                .addShape(nSamples)
                .addShape(nFeatures)
                .setDatatype(DEFAULT_DATATYPE.toString())
                .setContents(contents);
        return tensor;
    }

    private ModelInferRequest.Builder buildRequest(ModelInferRequest.InferInputTensor.Builder tensor) {
        final ModelInferRequest.Builder request = ModelInferRequest
                .newBuilder()
                .setModelName(this.modelName);

        request.addInputs(tensor);
        return request;
    }

    private PredictionOutput fromContentList(List<?> values, Type type) {
        final int size = values.size();
        List<String> names = this.outputNames == null ? IntStream.range(0, size).mapToObj(i -> "output-" + i).collect(Collectors.toList()) : this.outputNames;
        if (names.size() != size) {
            throw new IllegalArgumentException("Output names list has an incorrect size (" + names.size() + ", when it should be " + size + ")");
        }
        return new PredictionOutput(IntStream
                .range(0, size)
                .mapToObj(i -> new Output(names.get(i), type, new Value(values.get(i)), 1.0))
                .collect(Collectors.toUnmodifiableList()));
    }

    private PredictionOutput responseOutputToPredictionOutput(ModelInferResponse.InferOutputTensor tensor) {
        final InferTensorContents responseOutputContents = tensor.getContents();

        final KServeDatatype type = KServeDatatype.valueOf(tensor.getDatatype());

        switch (type) {
            case BOOL:
                return fromContentList(responseOutputContents.getBoolContentsList(), Type.BOOLEAN);
            case INT8:
            case INT16:
            case INT32:
                return fromContentList(responseOutputContents.getIntContentsList(), Type.NUMBER);
            case INT64:
                return fromContentList(responseOutputContents.getInt64ContentsList(), Type.NUMBER);
            case FP32:
            case FP64:
                return fromContentList(responseOutputContents.getFp64ContentsList(), Type.NUMBER);
            default:
                throw new IllegalArgumentException("Currently unsupported type for Tensor output.");
        }
    }

    private List<PredictionOutput> responseToPredictionOutput(ModelInferResponse response) {

        final List<ModelInferResponse.InferOutputTensor> responseOutputs = response.getOutputsList();

        return responseOutputs.stream().map(this::responseOutputToPredictionOutput).collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<List<PredictionOutput>> predictAsync(List<PredictionInput> inputs) {

        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Prediction inputs must not be empty.");
        }

        final int nFeatures = inputs.get(0).getFeatures().size();

        if (nFeatures == 0) {
            throw new IllegalArgumentException("Prediction inputs must have at least one feature.");
        }

        final InferTensorContents.Builder contents = buildTensorContents(inputs);

        final ModelInferRequest.InferInputTensor.Builder tensor = buildTensor(contents, inputs.size(), nFeatures);

        final ModelInferRequest.Builder request = buildRequest(tensor);

        final GRPCInferenceServiceGrpc.GRPCInferenceServiceFutureStub futureStub = GRPCInferenceServiceGrpc.newFutureStub(channel);

        final ListenableFuture<ModelInferResponse> listenableResponse = futureStub.modelInfer(request.build());

        final CompletableFuture<ModelInferResponse> futureResponse = ListenableFutureUtils.asCompletableFuture(listenableResponse);

        return futureResponse.thenApply(this::responseToPredictionOutput);
    }

}
