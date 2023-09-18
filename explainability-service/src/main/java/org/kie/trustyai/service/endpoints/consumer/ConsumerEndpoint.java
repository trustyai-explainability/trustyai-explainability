package org.kie.trustyai.service.endpoints.consumer;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
import org.kie.trustyai.connectors.kserve.v2.TensorConverter;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.utils.InferencePayloadReconciler;
import org.kie.trustyai.service.payloads.consumer.InferencePartialPayload;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;
import org.kie.trustyai.service.payloads.consumer.PartialKind;
import org.kie.trustyai.service.payloads.consumer.upload.ModelInferJointPayload;
import org.kie.trustyai.service.payloads.consumer.upload.UploadUtils;

@Path("/consumer/kserve/v2")
public class ConsumerEndpoint {

    private static final Logger LOG = Logger.getLogger(ConsumerEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    InferencePayloadReconciler reconciler;

    @POST
    @Path("/logger")
    public void logInference(Object jsonEvent) {
        LOG.info(jsonEvent);
    }

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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/upload")
    public Response directConsumption(ModelInferJointPayload jointPayload) throws DataframeCreateException {
        if (jointPayload.getRequest() == null || jointPayload.getRequest().getInputs().length < 1) {
            return Response.serverError().entity("Directly uploaded datapoints must specify at least one `inputs` field.").status(Response.Status.BAD_REQUEST).build();
        }

        // parse inputs
        ModelInferRequest.Builder inferRequestBuilder = ModelInferRequest.newBuilder();
        inferRequestBuilder.setModelName(jointPayload.getModelName());
        try {
            UploadUtils.populateRequestBuilder(inferRequestBuilder, jointPayload.getRequest());
        } catch (IllegalArgumentException e) {
            return Response.serverError().entity(e.getMessage()).status(Response.Status.BAD_REQUEST).build();
        }

        List<PredictionInput> predictionInputs;
        try {
            predictionInputs = TensorConverter.parseKserveModelInferRequest(inferRequestBuilder.build());
        } catch (IllegalArgumentException e) {
            throw new DataframeCreateException("Error parsing input payload: " + e.getMessage());
        }

        List<PredictionOutput> predictionOutputs;
        if (jointPayload.getResponse() != null && jointPayload.getResponse().getOutputs().length > 0) {
            // parse outputs
            ModelInferResponse.Builder inferResponseBuilder = ModelInferResponse.newBuilder();
            inferResponseBuilder.setModelName(jointPayload.getModelName());
            try {
                UploadUtils.populateResponseBuilder(inferResponseBuilder, jointPayload.getResponse());
            } catch (IllegalArgumentException e) {
                return Response.serverError().entity(e.getMessage()).status(Response.Status.BAD_REQUEST).build();
            }

            try {
                predictionOutputs = TensorConverter.parseKserveModelInferResponse(inferResponseBuilder.build(), predictionInputs.size());
            } catch (IllegalArgumentException e) {
                throw new DataframeCreateException("Error parsing output payload: " + e.getMessage());
            }
        } else {
            //todo, grab automatically from model
            throw new IllegalArgumentException("No output payload specified in request");
        }

        final List<Prediction> predictions =
                IntStream.range(0, predictionInputs.size()).mapToObj(i -> new SimplePrediction(predictionInputs.get(i), predictionOutputs.get(i))).collect(Collectors.toList());
        final Dataframe dataframe = Dataframe.createFrom(predictions);

        // tag the points accordingly
        DatapointSource dpSource;
        if (jointPayload.getDataTag() == null) {
            dpSource = DatapointSource.UNLABELED;
        } else {
            try {
                dpSource = DatapointSource.valueOf(jointPayload.getDataTag());
            } catch (IllegalArgumentException e) {
                return Response.serverError()
                        .entity("Provided datapoint tag=" + jointPayload.getDataTag() + " is not valid. Must be one of " + Arrays.toString(DatapointSource.values()))
                        .status(Response.Status.BAD_REQUEST)
                        .build();
            }
        }

        EnumMap<DatapointSource, List<List<Integer>>> taggingMap = new EnumMap<>(DatapointSource.class);
        taggingMap.put(dpSource, List.of(List.of(0, dataframe.getRowDimension())));
        dataframe.tagDataPoints(taggingMap);

        dataSource.get().saveDataframe(dataframe, jointPayload.getModelName());
        return Response.ok().entity(predictions.size() + " datapoints successfully added to " + jointPayload.getModelName() + " data.").build();
    }

}
