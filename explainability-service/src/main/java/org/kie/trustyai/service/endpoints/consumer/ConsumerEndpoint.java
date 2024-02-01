package org.kie.trustyai.service.endpoints.consumer;

import java.util.Base64;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.reconcilers.ModelMeshInferencePayloadReconciler;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/consumer/kserve/v2")
public class ConsumerEndpoint {

    private static final Logger LOG = Logger.getLogger(ConsumerEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    ModelMeshInferencePayloadReconciler reconciler;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/full")
    public Response consume(InferencePayload request) throws DataframeCreateException {
        LOG.debug("Got full payload on the consumer");

        final String modelId = request.getModelId();

        final byte[] inputBytes = Base64.getDecoder().decode(request.getInput().getBytes());
        final byte[] outputBytes = Base64.getDecoder().decode(request.getOutput().getBytes());

        Dataframe dataframe;

        try {
            dataframe = reconciler.payloadToDataFrame(inputBytes, outputBytes, String.valueOf(UUID.randomUUID()),
                    request.getMetadata(), modelId);
        } catch (DataframeCreateException e) {

            LOG.error("Could not create dataframe from payloads: " + e.getMessage());
            return Response.serverError().status(RestResponse.StatusCode.INTERNAL_SERVER_ERROR).build();
        }

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
