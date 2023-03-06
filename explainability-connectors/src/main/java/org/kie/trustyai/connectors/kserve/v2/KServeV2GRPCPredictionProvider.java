package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.kie.trustyai.connectors.kserve.v2.grpc.GRPCInferenceServiceGrpc;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.connectors.utils.ListenableFutureUtils;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;

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

    private List<PredictionOutput> responseToPredictionOutput(ModelInferResponse response) {

        final List<ModelInferResponse.InferOutputTensor> responseOutputs = response.getOutputsList();

        return responseOutputs
                .stream()
                .map(tensor -> PayloadParser.outputTensorToPredictionOutput(tensor, this.outputNames))
                .collect(Collectors.toList());
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

        final InferTensorContents.Builder contents = PayloadParser.predictionInputToTensorContents(inputs);

        final ModelInferRequest.InferInputTensor.Builder tensor = buildTensor(contents, inputs.size(), nFeatures);

        final ModelInferRequest.Builder request = buildRequest(tensor);

        final GRPCInferenceServiceGrpc.GRPCInferenceServiceFutureStub futureStub = GRPCInferenceServiceGrpc.newFutureStub(channel);

        final ListenableFuture<ModelInferResponse> listenableResponse = futureStub.modelInfer(request.build());

        final CompletableFuture<ModelInferResponse> futureResponse = ListenableFutureUtils.asCompletableFuture(listenableResponse);

        return futureResponse.thenApply(this::responseToPredictionOutput);
    }

}
