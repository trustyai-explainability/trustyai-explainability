package org.kie.trustyai.service.endpoints.consumer;

import java.util.Base64;

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
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.utils.InferencePayloadReconciler;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;

@Path("/consumer/kserve/v2")
public class ConsumerEndpoint {

    private static final Logger LOG = Logger.getLogger(ConsumerEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    InferencePayloadReconciler reconciler;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/full")
    public Response consume(InferencePayload request) throws DataframeCreateException {
        LOG.debug("Got full payload on the consumer");

        final String modelId = request.getModelId();

        final byte[] inputBytes = Base64.getDecoder().decode(request.getInput().getBytes());
        final byte[] outputBytes = Base64.getDecoder().decode(request.getOutput().getBytes());

        final Prediction prediction;
        try {
            prediction = reconciler.payloadToPrediction(inputBytes, outputBytes);
        } catch (DataframeCreateException e) {
            LOG.error("Could not create dataframe from payloads: " + e.getMessage());
            return Response.serverError().status(RestResponse.StatusCode.INTERNAL_SERVER_ERROR).build();
        }

        final Dataframe dataframe = Dataframe.createFrom(prediction);

        // Save data
        dataSource.get().saveDataframe(dataframe, modelId);

        try {
            dataSource.get().updateMetadataObservations(dataframe.getRowDimension(), modelId);
        } catch (StorageWriteException e) {
            LOG.error("Error saving metadata for model " + modelId + ": " + e.getMessage());
            return Response.serverError().status(RestResponse.StatusCode.INTERNAL_SERVER_ERROR).build();
        }

        return Response.ok().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response consumeInput(InferencePartialPayload request) throws DataframeCreateException {

        if (request.getKind().equals(PartialKind.request)) {
            LOG.info("Received partial input payload from model='" + request.getModelId() + "', id=" + request.getId());
            try {
                reconciler.addUnreconciledInput(request);
            } catch (InvalidSchemaException | DataframeCreateException e) {
                final String message = "Invalid schema for payload request id=" + request.getId() + ", " + e.getMessage();
                LOG.error(message);
                return Response.serverError().entity(message).status(Response.Status.BAD_REQUEST).build();
            }
        } else if (request.getKind().equals(PartialKind.response)) {
            LOG.info("Received partial output payload from model='" + request.getModelId() + "', id=" + request.getId());
            try {
                reconciler.addUnreconciledOutput(request);
            } catch (InvalidSchemaException | DataframeCreateException e) {
                final String message = "Invalid schema for payload response id=" + request.getId() + ", " + e.getMessage();
                LOG.error(message);
                return Response.serverError().entity(message).status(Response.Status.BAD_REQUEST).build();
            }
        } else {
            return Response.serverError().entity("Unsupported payload kind=" + request.getKind()).status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }
}
