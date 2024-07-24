package org.kie.trustyai.service.data.reconcilers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.reconcilers.payloadstorage.kserve.KServePayloadStorage;
import org.kie.trustyai.service.payloads.consumer.InferenceLoggerGeneral;
import org.kie.trustyai.service.payloads.consumer.InferenceLoggerInput;
import org.kie.trustyai.service.payloads.consumer.InferenceLoggerOutput;
import org.kie.trustyai.service.payloads.consumer.partial.KServeInputPayload;
import org.kie.trustyai.service.payloads.consumer.partial.KServeOutputPayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
        final Dataframe dataframe = payloadToDataframe(input, output, id, null);

        datasource.get().saveDataframe(dataframe, modelId);

        kservePayloadStorage.get().removeUnreconciledInput(id);
        kservePayloadStorage.get().removeUnreconciledOutput(id);
    }

    public Dataframe payloadToDataframe(KServeInputPayload inputs, KServeOutputPayload outputs, String id, Map<String, String> metadata) throws DataframeCreateException {

        final ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode;
        List<Double> instanceData = null;

        try {
            rootNode = objectMapper.readTree(inputs.getData());

            if (rootNode.has("instances")) {
                // Handle the instances format
                InferenceLoggerInput originalFormat = objectMapper.treeToValue(rootNode, InferenceLoggerInput.class);
                instanceData = originalFormat.getInstances().get(0);
            } else if (rootNode.has("inputs")) {
                // Handle the inputs format
                InferenceLoggerGeneral newFormat = objectMapper.treeToValue(rootNode, InferenceLoggerGeneral.class);
                instanceData = newFormat.getInputs().get(0).getInputData().get(0);
            } else {
                throw new DataframeCreateException("Unknown input format");
            }
        } catch (JsonProcessingException e) {
            final String message = "Could not parse input data: " + e.getMessage();
            LOG.error(message);
            throw new DataframeCreateException(message);
        }

        final List<Double> data = instanceData;
        final PredictionInput predictionInput = new PredictionInput(IntStream.range(0, instanceData.size()).mapToObj(i -> {
            return FeatureFactory.newNumericalFeature("feature-" + i, data.get(i));
        }).collect(Collectors.toList()));

        InferenceLoggerOutput ilo = outputs.getData();
        final PredictionOutput predictionOutput = new PredictionOutput(IntStream.range(0, ilo.getPredictions().size()).mapToObj(i -> {
            return new Output("output-" + i, Type.NUMBER, new Value(ilo.getPredictions().get(i)), 1.0);
        }).collect(Collectors.toList()));

        final List<Prediction> predictions = List.of(new SimplePrediction(predictionInput, predictionOutput));

        return Dataframe.createFrom(predictions);
    }

}
