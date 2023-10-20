package org.kie.trustyai.service.data.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.values.DataType;

import com.google.protobuf.InvalidProtocolBufferException;

/**
 * Reconcile partial input and output inference payloads in the KServe v2 protobuf format.
 */
@Singleton
public class ModelMeshInferencePayloadReconciler extends InferencePayloadReconciler<InferencePartialPayload, InferencePartialPayload> {
    private static final Logger LOG = Logger.getLogger(ModelMeshInferencePayloadReconciler.class);

    @Inject
    Instance<DataSource> datasource;

    protected static final String MM_MODEL_SUFFIX = "__isvc";

    protected static String standardizeModelId(String inboundModelId) {
        if (inboundModelId != null && inboundModelId.contains(MM_MODEL_SUFFIX)) {
            int index = inboundModelId.lastIndexOf(MM_MODEL_SUFFIX);
            return inboundModelId.substring(0, index);
        } else {
            return inboundModelId;
        }
    }

    protected synchronized void save(String id, String modelId) throws InvalidSchemaException, DataframeCreateException {
        final InferencePartialPayload output = unreconciledOutputs.get(id);
        final InferencePartialPayload input = unreconciledInputs.get(id);
        LOG.debug("Reconciling partial input and output, id=" + id);

        // save
        final OptionallyTypedPredictionList optionallyTypedPredictionList = payloadToPrediction(input, output, id, input.getMetadata());
        final Dataframe dataframe = Dataframe.createFrom(optionallyTypedPredictionList.predictions);

        datasource.get().saveDataframe(dataframe, standardizeModelId(modelId), optionallyTypedPredictionList.types);

        unreconciledInputs.remove(id);
        unreconciledOutputs.remove(id);
    }

    /**
     * Convert both input and output {@link InferencePartialPayload} to a TrustyAI {@link Prediction}.
     * 
     * @param inputPayload Input {@link InferencePartialPayload}
     * @param outputPayload Output {@link InferencePartialPayload}
     * @param id The unique id of the payload
     * @return A {@link Prediction}
     * @throws DataframeCreateException
     */
    public OptionallyTypedPredictionList payloadToPrediction(InferencePartialPayload inputPayload, InferencePartialPayload outputPayload, String id, Map<String, String> metadata)
            throws DataframeCreateException {
        final byte[] inputBytes = Base64.getDecoder().decode(inputPayload.getData().getBytes());
        final byte[] outputBytes = Base64.getDecoder().decode(outputPayload.getData().getBytes());

        final ModelInferRequest input;
        try {
            input = ModelInferRequest.parseFrom(inputBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final List<PredictionInput> predictionInput;
        List<DataType> types = null;
        final int enforcedFirstDimension;
        try {
            predictionInput = TensorConverter.parseKserveModelInferRequest(input);
            enforcedFirstDimension = predictionInput.size();
            if (predictionInput.size() == 1) {
                List<DataType> candidateInputTypes = input.getInputsList().stream().map(iit -> {
                    DataType dt = PayloadConverter.payloadTypeToDataType(iit.getDatatype());
                    int listSizeProduct = iit.getShapeList().stream().reduce(1L, (x, y) -> x * y).intValue();
                    return Collections.nCopies(listSizeProduct, dt);
                })
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
                if (candidateInputTypes.size() == predictionInput.get(0).getFeatures().size()) {
                    types = candidateInputTypes;
                }
            }
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing input payload: " + e.getMessage());
        }
        LOG.debug("Prediction input: " + predictionInput);
        final ModelInferResponse output;
        try {
            output = ModelInferResponse.parseFrom(outputBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final List<PredictionOutput> predictionOutput;
        try {
            predictionOutput = TensorConverter.parseKserveModelInferResponse(output, enforcedFirstDimension);
            if (predictionOutput.size() == 1) {
                List<DataType> candidateOutputTypes = output.getOutputsList().stream().map(iot -> {
                    DataType dt = PayloadConverter.payloadTypeToDataType(iot.getDatatype());
                    int listSizeProduct = iot.getShapeList().stream().reduce(1L, (x, y) -> x * y).intValue();
                    return Collections.nCopies(listSizeProduct, dt);
                })
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
                if (candidateOutputTypes.size() == predictionOutput.get(0).getOutputs().size() && types != null) {
                    types.addAll(candidateOutputTypes);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing output payload: " + e.getMessage());
        }

        LOG.debug("Prediction output: " + predictionOutput);

        // Aggregate features and outputs
        final int size = predictionInput.size();
        return new OptionallyTypedPredictionList(types, IntStream.range(0, size).mapToObj(i -> {
            final PredictionInput pi = predictionInput.get(i);
            final PredictionOutput po = predictionOutput.get(i);
            return new SimplePrediction(pi, po);
        }).collect(Collectors.toCollection(ArrayList::new)));
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

        String datapointTag = metadata.containsKey(ExplainerEndpoint.BIAS_IGNORE_PARAM) ? Dataframe.InternalTags.SYNTHETIC.get() : Dataframe.InternalTags.UNLABELED.get();
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
