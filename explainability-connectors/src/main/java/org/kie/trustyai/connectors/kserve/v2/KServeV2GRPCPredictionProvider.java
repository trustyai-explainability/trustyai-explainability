package org.kie.trustyai.connectors.kserve.v2;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.kie.trustyai.connectors.kserve.v2.grpc.GRPCInferenceServiceGrpc;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferParameter;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.connectors.utils.ListenableFutureUtils;
import org.kie.trustyai.explainability.model.*;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Wraps a KServe v2-compatible model server as a TrustyAI {@link PredictionProvider}
 */
public class KServeV2GRPCPredictionProvider implements PredictionProvider {

    private static final String DEFAULT_TENSOR_NAME = "predict";
    private final KServeConfig kServeConfig;
    private final ManagedChannel channel;
    private final List<String> outputNames;

    private KServeV2GRPCPredictionProvider(KServeConfig kServeConfig, List<String> outputNames) {

        this.kServeConfig = kServeConfig;
        this.channel = ManagedChannelBuilder.forTarget(kServeConfig.getTarget())
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
    public static KServeV2GRPCPredictionProvider forTarget(KServeConfig kServeConfig) {
        return KServeV2GRPCPredictionProvider.forTarget(kServeConfig, null);
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
    public static KServeV2GRPCPredictionProvider forTarget(KServeConfig kServeConfig, List<String> outputNames) {
        return new KServeV2GRPCPredictionProvider(kServeConfig, outputNames);
    }

    private List<PredictionOutput> responseToPredictionOutput(ModelInferResponse response) {

        final List<ModelInferResponse.InferOutputTensor> responseOutputs = response.getOutputsList();

        final List<List<Output>> shapedOutputs = new ArrayList<>();

        final int columns = (int) responseOutputs.get(0).getShape(1);
        final AtomicInteger counter = new AtomicInteger();
        final ModelInferResponse.InferOutputTensor tensor = responseOutputs.get(0);
        final List<Output> outputs = TensorConverter.outputTensorToOutputs(tensor, null,  null);

        for (Output output : outputs) {
            if (counter.getAndIncrement() % columns == 0) {
                shapedOutputs.add(new ArrayList<>());
            }
            shapedOutputs.get(shapedOutputs.size() - 1).add(output);
        }

        return shapedOutputs.stream().map(PredictionOutput::new).collect(Collectors.toList());
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

        final TensorDataframe tdf = TensorDataframe.createFromInputs(inputs);

        final ModelInferRequest.Builder request = ModelInferRequest.newBuilder();

        if (this.kServeConfig.getCodec() == CodecParameter.NP) {

            request.addInputs(tdf.rowAsSingleArrayInputTensor(0, DEFAULT_TENSOR_NAME));

        } else {

            tdf.asBatchDataframeInputTensor().forEach(request::addInputs);

            request.putParameters("content_type", InferParameter.newBuilder().setStringParam("pd").build());
        }
        request.setModelNameBytes(ByteString.copyFromUtf8(this.kServeConfig.getModelId()));
        request.setModelVersionBytes(ByteString.copyFromUtf8(this.kServeConfig.getVersion()));

        final GRPCInferenceServiceGrpc.GRPCInferenceServiceFutureStub futureStub = GRPCInferenceServiceGrpc.newFutureStub(channel);

        final ListenableFuture<ModelInferResponse> listenableResponse = futureStub.modelInfer(request.build());

        final CompletableFuture<ModelInferResponse> futureResponse = ListenableFutureUtils.asCompletableFuture(listenableResponse);

        return futureResponse.thenApply(this::responseToPredictionOutput);

    }

}
