package org.kie.trustyai.service.data.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.PayloadParser;
import org.kie.trustyai.connectors.kserve.v2.TensorConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Reconcile partial input and output inference payloads in the KServe v2 protobuf format.
 */
@Singleton
public class InferencePayloadReconciler {
    private static final Logger LOG = Logger.getLogger(InferencePayloadReconciler.class);
    private final Map<String, InferencePartialPayload> unreconciledInputs = new ConcurrentHashMap<>();
    private final Map<String, InferencePartialPayload> unreconciledOutputs = new ConcurrentHashMap<>();

    @Inject
    Instance<DataSource> datasource;

    /**
     * Add a {@link InferencePartialPayload} input to the (yet) unreconciled mapping.
     * If there is a corresponding (based on unique id) output {@link InferencePartialPayload},
     * both are saved to storage and removed from the unreconciled mapping.
     * 
     * @param input
     */
    public synchronized void addUnreconciledInput(InferencePartialPayload input) throws InvalidSchemaException, DataframeCreateException {
        final String id = input.getId();
        unreconciledInputs.put(id, input);
        if (unreconciledOutputs.containsKey(id)) {
            save(id, input.getModelId());
        }
    }

    /**
     * Add a {@link InferencePartialPayload} output to the (yet) unreconciled mapping.
     * If there is a corresponding (based on unique id) input {@link InferencePartialPayload},
     * both are saved to storage and removed from the unreconciled mapping.
     * 
     * @param output
     */
    public synchronized void addUnreconciledOutput(InferencePartialPayload output) throws InvalidSchemaException, DataframeCreateException {
        final String id = output.getId();
        unreconciledOutputs.put(id, output);
        if (unreconciledInputs.containsKey(id)) {
            save(id, output.getModelId());
        }
    }

    private synchronized void save(String id, String modelId) throws InvalidSchemaException, DataframeCreateException {
        final InferencePartialPayload output = unreconciledOutputs.get(id);
        final InferencePartialPayload input = unreconciledInputs.get(id);
        LOG.debug("Reconciling partial input and output, id=" + id);

        // save
        final byte[] inputBytes = Base64.getDecoder().decode(input.getData().getBytes());
        final byte[] outputBytes = Base64.getDecoder().decode(output.getData().getBytes());

        final List<Prediction> prediction = payloadToPrediction(inputBytes, outputBytes, id, input.getMetadata());
        final Dataframe dataframe = Dataframe.createFrom(prediction);

        datasource.get().saveDataframe(dataframe, modelId);

        unreconciledInputs.remove(id);
        unreconciledOutputs.remove(id);
    }

    /**
     * Convert both input and output {@link InferencePartialPayload} to a TrustyAI {@link Prediction}.
     * 
     * @param inputs KServe v2 protobuf raw bytes
     * @param outputs KServe v2 protobuf raw bytes
     * @param id The unique id of the payload
     * @return A {@link Prediction}
     * @throws DataframeCreateException
     */
    public List<Prediction> payloadToPrediction(byte[] inputs, byte[] outputs, String id, Map<String, String> metadata) throws DataframeCreateException {
        final ModelInferRequest input;
        try {
            input = ModelInferRequest.parseFrom(inputs);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final List<PredictionInput> predictionInput;
        final int enforcedFirstDimension;
        try {
            predictionInput = TensorConverter.parseKserveModelInferRequest(input);
            enforcedFirstDimension = predictionInput.size();
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing input payload: " + e.getMessage());
        }
        LOG.debug("Prediction input: " + predictionInput);
        final ModelInferResponse output;
        try {
            output = ModelInferResponse.parseFrom(outputs);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final List<PredictionOutput> predictionOutput;
        try {
            predictionOutput = TensorConverter.parseKserveModelInferResponse(output, enforcedFirstDimension);
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing output payload: " + e.getMessage());
        }
        LOG.debug("Prediction output: " + predictionOutput);

        // Aggregate features and outputs
        final int size = predictionInput.size();
        return IntStream.range(0, size).mapToObj(i -> {
            final PredictionInput pi = predictionInput.get(i);
            final PredictionOutput po = predictionOutput.get(i);
            return new SimplePrediction(pi, po);
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    public Dataframe payloadToDataFrame(byte[] inputs, byte[] outputs, String id, Map<String, String> metadata,
            String modelId) throws DataframeCreateException {
        final ModelInferRequest input;
        try {
            input = ModelInferRequest.parseFrom(inputs);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final PredictionInput predictionInput;
        try {
            predictionInput = PayloadParser
                    .requestToInput(input, null);
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing input payload: " + e.getMessage());
        }
        LOG.debug("Prediction input: " + predictionInput.getFeatures());
        final List<Feature> features = new ArrayList<>(predictionInput.getFeatures());

        String datapointTag = metadata.containsKey(ExplainerEndpoint.BIAS_IGNORE_PARAM) ? Dataframe.InternalTags.SYNTHETIC.get() : "";
        PredictionMetadata predictionMetadata = new PredictionMetadata(id, LocalDateTime.now(), datapointTag);

        final ModelInferResponse output;
        try {
            output = ModelInferResponse.parseFrom(outputs);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final PredictionOutput predictionOutput;
        try {
            predictionOutput = PayloadParser
                    .responseToOutput(output, null);
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing output payload: " + e.getMessage());
        }
        LOG.debug("Prediction output: " + predictionOutput.getOutputs());

        Prediction prediction = new SimplePrediction(new PredictionInput(features), predictionOutput);
        return Dataframe.createFrom(prediction, predictionMetadata);
    }
}
