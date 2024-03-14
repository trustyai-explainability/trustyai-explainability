package org.kie.trustyai.service.mocks;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.kie.trustyai.connectors.kserve.v2.TensorConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.GRPCInferenceServiceGrpc;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;

import io.grpc.stub.StreamObserver;

public class MockInferenceServiceImpl extends GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase {
    private static final Logger logger = Logger.getLogger(MockInferenceServiceImpl.class.getName());
    private final PredictionProvider predictionProvider;

    public MockInferenceServiceImpl(PredictionProvider predictionProvider) {
        this.predictionProvider = predictionProvider;
    }

    private ModelInferResponse convertToModelInferResponse(List<PredictionOutput> predictionOutputs) {
        ModelInferResponse.Builder responseBuilder = ModelInferResponse.newBuilder();

        InferTensorContents.Builder tensorContentsBuilder = InferTensorContents.newBuilder();

        for (PredictionOutput predictionOutput : predictionOutputs) {
            for (Output output : predictionOutput.getOutputs()) {
                double value = output.getValue().asNumber();
                tensorContentsBuilder.addFp64Contents(value);
            }
        }
        ModelInferResponse.InferOutputTensor.Builder tensorBuilder = ModelInferResponse.InferOutputTensor.newBuilder()
                .setName(DataframeMetadata.DEFAULT_OUTPUT_TENSOR_NAME)
                .setDatatype("FP64")
                .setContents(tensorContentsBuilder)
                .addShape(predictionOutputs.size())
                .addShape(predictionOutputs.get(0).getOutputs().size());

        responseBuilder.addOutputs(tensorBuilder);

        return responseBuilder.build();
    }

    @Override
    public void modelInfer(ModelInferRequest request, StreamObserver<ModelInferResponse> responseObserver) {

        try {
            final List<PredictionInput> inputs = TensorConverter.parseKserveModelInferRequest(request);
            final List<PredictionOutput> outputs = predictionProvider.predictAsync(inputs).get();
            ModelInferResponse response = convertToModelInferResponse(outputs);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Failed to process model inference request: " + e.getMessage());
            responseObserver.onError(new RuntimeException("Error processing request", e));
        }
    }
}
