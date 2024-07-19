package org.kie.trustyai.service.data.reconcilers;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.TensorConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;

import com.google.protobuf.InvalidProtocolBufferException;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Reconcile partial input and output inference payloads in the KServe v2 protobuf format.
 */
@Singleton
public class ModelMeshInferencePayloadReconciler extends InferencePayloadReconciler<InferencePartialPayload, InferencePartialPayload> {
    protected static final String MM_MODEL_SUFFIX = "__isvc";
    private static final Logger LOG = Logger.getLogger(ModelMeshInferencePayloadReconciler.class);
    @Inject
    Instance<DataSource> datasource;

    protected static String standardizeModelId(String inboundModelId) {
        if (inboundModelId != null && inboundModelId.contains(MM_MODEL_SUFFIX)) {
            int index = inboundModelId.lastIndexOf(MM_MODEL_SUFFIX);
            return inboundModelId.substring(0, index);
        } else {
            return inboundModelId;
        }
    }

    protected synchronized void save(String id, String modelId) throws InvalidSchemaException, DataframeCreateException {
        final InferencePartialPayload output = payloadStorage.get().getUnreconciledOutput(id);
        final InferencePartialPayload input = payloadStorage.get().getUnreconciledInput(id);
        LOG.debug("Reconciling partial input and output, id=" + id);

        // save
        final Dataframe dataframe = payloadToDataframe(input, output, id, input.getMetadata());
        datasource.get().saveDataframe(dataframe, standardizeModelId(modelId));

        payloadStorage.get().removeUnreconciledInput(id);
        payloadStorage.get().removeUnreconciledOutput(id);
    }

    /**
     * Convert both input and output {@link InferencePartialPayload} to a TrustyAI {@link Prediction}.
     * If the input tensor contains a {@link ExplainerEndpoint#BIAS_IGNORE_PARAM} set, then {@link PredictionMetadata}
     * will be attached marking these inferences as synthetic.
     *
     * @param inputPayload Input {@link InferencePartialPayload}
     * @param outputPayload Output {@link InferencePartialPayload}
     * @param id The unique id of the payload
     * @return A {@link Prediction}
     * @throws DataframeCreateException
     */
    public Dataframe payloadToDataframe(InferencePartialPayload inputPayload, InferencePartialPayload outputPayload, String id, Map<String, String> metadata) throws DataframeCreateException {
        final byte[] inputBytes = Base64.getDecoder().decode(inputPayload.getData().getBytes());
        final byte[] outputBytes = Base64.getDecoder().decode(outputPayload.getData().getBytes());

        final ModelInferRequest input;
        try {
            input = ModelInferRequest.parseFrom(inputBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        // Construct record of synthetic data tags
        List<Dataframe.InternalTags> tags = input.getInputsList().stream().map(inferInputTensor -> {
            if (inferInputTensor.containsParameters(ExplainerEndpoint.BIAS_IGNORE_PARAM)
                    && inferInputTensor.getParametersMap().get(ExplainerEndpoint.BIAS_IGNORE_PARAM).getStringParam().equals("true")) {
                return Dataframe.InternalTags.SYNTHETIC;
            } else {
                return Dataframe.InternalTags.UNLABELED;
            }
        }).collect(Collectors.toList());
        LOG.debug("Payload tags: " + tags);

        final List<PredictionInput> predictionInputs;
        final int enforcedFirstDimension;
        try {
            predictionInputs = TensorConverter.parseKserveModelInferRequest(input);
            enforcedFirstDimension = predictionInputs.size();
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing input payload: " + e.getMessage());
        }
        LOG.debug("Prediction input: " + predictionInputs);
        final ModelInferResponse output;
        try {
            output = ModelInferResponse.parseFrom(outputBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new DataframeCreateException(e.getMessage());
        }
        final List<PredictionOutput> predictionOutputs;
        try {
            predictionOutputs = TensorConverter.parseKserveModelInferResponse(output, enforcedFirstDimension);
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing output payload: " + e.getMessage());
        }
        LOG.debug("Prediction output: " + predictionOutputs);

        // Aggregate features and outputs
        final int size = predictionInputs.size();
        // Use the tensor tag for a batch
        // If no tags are recorded, use unlabelled by default
        final String tag = tags.isEmpty() ? Dataframe.InternalTags.UNLABELED.get() : tags.get(0).get();
        final List<Prediction> predictions = IntStream.range(0, size).mapToObj(i -> {
            final PredictionInput pi = predictionInputs.get(i);
            final PredictionOutput po = predictionOutputs.get(i);
            final PredictionMetadata predictionMetadata = PredictionMetadata.fromTag(tag);
            return (Prediction) new SimplePrediction(pi, po, predictionMetadata);

        }).collect(Collectors.toCollection(ArrayList::new));

        final Dataframe dataframe = Dataframe.createFrom(predictions);

        dataframe.setInputTensorName(input.getInputs(0).getName());
        dataframe.setOutputTensorName(output.getOutputs(0).getName());

        return dataframe;
    }
}
