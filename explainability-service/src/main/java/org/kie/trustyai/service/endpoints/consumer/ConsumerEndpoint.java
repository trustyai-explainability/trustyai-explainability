package org.kie.trustyai.service.endpoints.consumer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.connectors.kserve.v2.PayloadParser;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;

@Path("/consumer/kserve/v2")
public class ConsumerEndpoint {

    private static final Logger LOG = Logger.getLogger(ConsumerEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response consume(InferencePayload request) throws DataframeCreateException {
        LOG.info("Got payload on the consumer");
        try {
            final String modelId = request.getModelId();

            final byte[] inputBytes = Base64.getDecoder().decode(request.getInput().getBytes());
            final ModelInferRequest input = ModelInferRequest.parseFrom(inputBytes);
            final PredictionInput predictionInput = PayloadParser
                    .inputTensorToPredictionInput(input.getInputs(0), null);
            LOG.info(predictionInput.getFeatures());

            // Check for dataframe metadata name conflicts
            if (predictionInput.getFeatures()
                    .stream()
                    .map(Feature::getName)
                    .anyMatch(name -> name.equals(MetadataUtils.ID_FIELD) || name.equals(MetadataUtils.TIMESTAMP_FIELD))) {
                final String message = "An input feature as a protected name: \"_id\" or \"_timestamp\"";
                LOG.error(message);
                return Response.serverError().status(Response.Status.BAD_REQUEST).entity(message).build();
            }

            // enrich with data and id
            final List<Feature> features = new ArrayList<>();
            features.add(FeatureFactory.newObjectFeature(MetadataUtils.ID_FIELD, UUID.randomUUID()));
            features.add(FeatureFactory.newObjectFeature(MetadataUtils.TIMESTAMP_FIELD, LocalDateTime.now()));
            features.addAll(predictionInput.getFeatures());

            final byte[] outputBytes = Base64.getDecoder().decode(request.getOutput().getBytes());
            final ModelInferResponse output = ModelInferResponse.parseFrom(outputBytes);
            final PredictionOutput predictionOutput = PayloadParser
                    .outputTensorToPredictionOutput(output.getOutputs(0), null);
            LOG.info(predictionOutput.getOutputs());
            final Prediction prediction = new SimplePrediction(new PredictionInput(features), predictionOutput);

            final Dataframe dataframe = Dataframe.createFrom(prediction);

            // Save data
            dataSource.get().saveDataframe(dataframe, modelId);

            // Save metadata if it doesn't exist
            final Metadata metadata;
            if (!dataSource.get().hasMetadata(modelId)) {

                metadata = new Metadata();

                metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
                metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
                metadata.setModelId(modelId);

            } else {
                try {
                    metadata = dataSource.get().getMetadata(modelId);
                } catch (JsonProcessingException e) {
                    throw new DataframeCreateException("Could not parse metadata for model " + modelId + ": " + e.getMessage());
                }
            }
            metadata.incrementObservations(dataframe.getRowDimension());

            try {
                dataSource.get().saveMetadata(metadata, modelId);
            } catch (JsonProcessingException e) {
                LOG.error("Error parsing metadata for model " + modelId + ": " + e.getMessage());
                return Response.serverError().status(RestResponse.StatusCode.INTERNAL_SERVER_ERROR).build();
            }

        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error parsing protobuf message: " + e.getMessage());
            return Response.serverError().status(RestResponse.StatusCode.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok().build();
    }

}
