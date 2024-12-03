package org.kie.trustyai.service.data.reconcilers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v1.KServeV1HTTPPayloadParser;
import org.kie.trustyai.connectors.kserve.v2.KServeV2HTTPPayloadParser;
import org.kie.trustyai.connectors.kserve.v2.TensorConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.reconcilers.payloadstorage.kserve.KServePayloadStorage;
import org.kie.trustyai.service.data.utils.KServePayloadConverters;
import org.kie.trustyai.service.data.utils.UploadUtils;
import org.kie.trustyai.service.payloads.consumer.partial.KServeInputPayload;
import org.kie.trustyai.service.payloads.consumer.partial.KServeOutputPayload;
import org.kie.trustyai.service.payloads.data.upload.ModelInferRequestPayload;
import org.kie.trustyai.service.payloads.data.upload.ModelInferResponsePayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Reconcile partial input and output inference payloads in the KServe v2 protobuf format.
 */
@ApplicationScoped
public class KServeInferencePayloadReconciler extends InferencePayloadReconciler<KServeInputPayload, KServeOutputPayload, KServePayloadStorage> {
    private static final Logger LOG = Logger.getLogger(KServeInferencePayloadReconciler.class);

    @Inject
    Instance<DataSource> datasource;

    @Inject
    Instance<KServePayloadStorage> kservePayloadStorage;

    @Inject
    ObjectMapper objectMapper;

    KServeV1HTTPPayloadParser v1PayloadParser = KServeV1HTTPPayloadParser.getInstance();
    KServeV2HTTPPayloadParser v2PayloadParser = KServeV2HTTPPayloadParser.getInstance();

    @Override
    KServePayloadStorage getPayloadStorage() {
        return kservePayloadStorage.get();
    }

    protected synchronized void save(String id, String modelId) throws InvalidSchemaException, DataframeCreateException {
        final KServeOutputPayload output = kservePayloadStorage.get().getUnreconciledOutput(id);
        final KServeInputPayload input = kservePayloadStorage.get().getUnreconciledInput(id);
        LOG.info("Reconciling partial input and output, id=" + id);

        // save
        LOG.info("Reconciling KServe payloads id = " + id);

        // Parse input
        // TODO: Add metadata support to KServe payloads and interface
        final Dataframe dataframe = payloadToDataframe(input, output, id, modelId, null);

        datasource.get().saveDataframe(dataframe, modelId);

        kservePayloadStorage.get().removeUnreconciledInput(id);
        kservePayloadStorage.get().removeUnreconciledOutput(id);
    }

    public Dataframe payloadToDataframe(KServeInputPayload inputs, KServeOutputPayload outputs, String id, String modelId, Map<String, String> metadata) throws DataframeCreateException {
        final ObjectMapper objectMapper = new ObjectMapper();
        List<PredictionInput> predictionInputs;
        List<PredictionOutput> predictionOutputs;
        try {
            // inputs
            ModelInferRequestPayload modelInferRequestPayload = KServePayloadConverters.toRequestPayload(inputs);
            ModelInferRequest.Builder inferRequestBuilder = ModelInferRequest.newBuilder();
            inferRequestBuilder.setModelName(modelId);
            UploadUtils.populateRequestBuilder(inferRequestBuilder, modelInferRequestPayload);
            predictionInputs = TensorConverter.parseKserveModelInferRequest(inferRequestBuilder.build());

            // outputs
            ModelInferResponsePayload modelInferResponsePayload = KServePayloadConverters.toResponsePayload(outputs);
            ModelInferResponse.Builder inferResponseBuilder = ModelInferResponse.newBuilder();
            inferResponseBuilder.setModelName(modelId);
            UploadUtils.populateResponseBuilder(inferResponseBuilder, modelInferResponsePayload);
            predictionOutputs = TensorConverter.parseKserveModelInferResponse(
                    inferResponseBuilder.build(),
                    predictionInputs.size());
        } catch (JsonProcessingException e) {
            final String message = "Could not parse input data: " + e.getMessage();
            LOG.error(message);
            throw new DataframeCreateException(message);
        }

        List<Prediction> predictions = IntStream.range(0, predictionInputs.size())
                .mapToObj(i -> new SimplePrediction(predictionInputs.get(i), predictionOutputs.get(i)))
                .collect(Collectors.toList());
        return Dataframe.createFrom(predictions);
    }

}
