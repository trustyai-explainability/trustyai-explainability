package org.kie.trustyai.connectors.kserve.v2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.kie.trustyai.connectors.kserve.v2.grpc.GRPCInferenceServiceGrpc;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferParameter;
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
    private final KServeConfig kServeConfig;
    private final ManagedChannel channel;
    private final List<String> outputNames;
    private final Map<String, String> optionalParameters;

    private KServeV2GRPCPredictionProvider(KServeConfig kServeConfig, List<String> outputNames, Map<String, String> optionalParameters) {

        this.kServeConfig = kServeConfig;
        this.channel = ManagedChannelBuilder.forTarget(kServeConfig.getTarget())
                .usePlaintext()
                .build();
        this.outputNames = outputNames;
        this.optionalParameters = optionalParameters;
    }

    /**
     * Create a {@link KServeV2GRPCPredictionProvider} for a model with an endpoint at {@code target} and named {@code modelName}.
     * In this case, the output names will be generated as {@code output-0}, {@code output-1}, ...
     *
     * @param target The remote KServe v2 model server
     * @param modelName The model's name
     * @return A {@link PredictionProvider}
     */
    public static KServeV2GRPCPredictionProvider forTarget(KServeConfig kServeConfig) {
        return KServeV2GRPCPredictionProvider.forTarget(kServeConfig, null, null);
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
    public static KServeV2GRPCPredictionProvider forTarget(KServeConfig kServeConfig, List<String> outputNames, Map<String, String> optionalParameters) {
        return new KServeV2GRPCPredictionProvider(kServeConfig, outputNames, null);
    }

    /**
     * Create a {@link KServeV2GRPCPredictionProvider} with an endpoint at {@code target} and named {@code modelName}.
     * In this case, the output names are specified with {@code outputNames}.
     *
     * @param target The remote KServe v2 model server
     * @param modelName The model's name
     * @param outputNames A {@link List} of output names to be used
     * @param parameters A {@link Map} of parameters to pass to the gRPC method call
     * @return A {@link PredictionProvider}
     */
    public static KServeV2GRPCPredictionProvider forTarget(KServeConfig kServeConfig, Map<String, String> parameters) {
        return new KServeV2GRPCPredictionProvider(kServeConfig, null, parameters);
    }

    private ModelInferRequest.InferInputTensor.Builder buildTensor(InferTensorContents.Builder contents, int nSamples, int nFeatures) {

        final ModelInferRequest.InferInputTensor.Builder tensor = ModelInferRequest.InferInputTensor
                .newBuilder();
        tensor.setName(DEFAULT_TENSOR_NAME)
                .addShape(nSamples)
                .addShape(nFeatures)
                .setDatatype(DEFAULT_DATATYPE.toString())
                .setContents(contents);
        if (this.optionalParameters != null) {
            Map<String, InferParameter> parameterMap = new HashMap<>();
            for (Map.Entry<String, String> entry : optionalParameters.entrySet()) {
                parameterMap.put(entry.getKey(), InferParameter.newBuilder().setStringParam(entry.getValue()).build());
            }
            tensor.putAllParameters(parameterMap);
        }
        return tensor;
    }

    private ModelInferRequest.Builder buildRequest(ModelInferRequest.InferInputTensor.Builder tensor) {
        final ModelInferRequest.Builder request = ModelInferRequest
                .newBuilder()
                .setModelName(this.kServeConfig.getModelId());

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

        return futureResponse.thenApply(response -> TensorConverter.parseKserveModelInferResponse(response, inputs.size(), Optional.of(this.outputNames)));
    }
}
