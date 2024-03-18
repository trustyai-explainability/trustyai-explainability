package org.kie.trustyai.service.mocks;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.kie.trustyai.connectors.kserve.v2.TensorConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.*;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;

import io.grpc.stub.StreamObserver;

public class MockInferenceServiceImpl extends GRPCInferenceServiceGrpc.GRPCInferenceServiceImplBase {
    private static final Logger logger = Logger.getLogger(MockInferenceServiceImpl.class.getName());
    private final PredictionProvider predictionProvider;

    public MockInferenceServiceImpl(PredictionProvider predictionProvider) {
        this.predictionProvider = predictionProvider;
    }

    private ModelInferResponse convertToModelInferResponse(List<PredictionOutput> predictionOutputs, boolean synthetic) {
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

        if (synthetic) {
            final InferParameter syntheticParam = InferParameter.getDefaultInstance().toBuilder().setStringParam("true").build();
            responseBuilder.putParameters(ExplainerEndpoint.BIAS_IGNORE_PARAM, syntheticParam);
        }

        responseBuilder.addOutputs(tensorBuilder);

        return responseBuilder.build();
    }

    @Override
    public void modelInfer(ModelInferRequest request, StreamObserver<ModelInferResponse> responseObserver) {

        try {
            final List<PredictionInput> inputs = TensorConverter.parseKserveModelInferRequest(request);
            final List<PredictionOutput> outputs = predictionProvider.predictAsync(inputs).get();
            ModelInferResponse response;
            if (request.getInputsList().get(0).containsParameters(ExplainerEndpoint.BIAS_IGNORE_PARAM)) {
                response = convertToModelInferResponse(outputs, true);
            } else {
                response = convertToModelInferResponse(outputs, false);
            }
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (InterruptedException | ExecutionException e) {
            logger.severe("Failed to process model inference request: " + e.getMessage());
            responseObserver.onError(new RuntimeException("Error processing request", e));
        }
    }
}
