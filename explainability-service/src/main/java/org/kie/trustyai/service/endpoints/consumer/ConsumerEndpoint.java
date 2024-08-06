package org.kie.trustyai.service.endpoints.consumer;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.PayloadWriteException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.reconcilers.ModelMeshInferencePayloadReconciler;
import org.kie.trustyai.service.payloads.consumer.partial.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.partial.PartialKind;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/consumer/kserve/v2")
@Tag(name = "{Internal Only} Inference Consumer",
        description = "This endpoint consumes inference payloads produced by ModelMesh-served models. While it's possible to manually interact with this endpoint, it is not recommended.")
public class ConsumerEndpoint {

    private static final Logger LOG = Logger.getLogger(ConsumerEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    ModelMeshInferencePayloadReconciler reconciler;

    @POST
    @Operation(summary = "Send a single ModelMesh input or output payload to TrustyAI.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response consumeInput(InferencePartialPayload request) throws DataframeCreateException {
        if (request.getKind().equals(PartialKind.request)) {
            LOG.info("Received partial input payload from model='" + request.getModelId() + "', id=" + request.getId());
            try {
                reconciler.addUnreconciledInput(request);
            } catch (InvalidSchemaException | DataframeCreateException | StorageWriteException e) {
                final String message = "Error when reconciling payload for request id='" + request.getId() + "': " + e.getMessage();
                LOG.error(message);
                return Response.serverError().entity(message).status(Response.Status.BAD_REQUEST).build();
            } catch (PayloadWriteException e) {
                final String message = e.getMessage();
                LOG.error(message);
                return Response.serverError().entity(message).status(Response.Status.BAD_REQUEST).build();
            }
        } else if (request.getKind().equals(PartialKind.response)) {
            LOG.info("Received partial output payload from model='" + request.getModelId() + "', id=" + request.getId());
            try {
                reconciler.addUnreconciledOutput(request);
            } catch (InvalidSchemaException | DataframeCreateException | StorageWriteException e) {
                final String message = "Error when reconciling payload for response id='" + request.getId() + "': " + e.getMessage();
                LOG.error(message);
                return Response.serverError().entity(message).status(Response.Status.BAD_REQUEST).build();
            } catch (PayloadWriteException e) {
                final String message = e.getMessage();
                LOG.error(message);
                return Response.serverError().entity(message).status(Response.Status.BAD_REQUEST).build();
            }
        } else {
            return Response.serverError().entity("Unsupported payload kind=" + request.getKind()).status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok().build();
    }
}
