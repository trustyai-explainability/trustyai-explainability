package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.endpoints.metrics.BaseEndpoint;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.drift.ValidDriftMetricRequest;

import io.quarkus.resteasy.reactive.server.EndpointDisabled;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@EndpointDisabled(name = "endpoints.drift", stringValue = "disable")
public abstract class DriftEndpoint<T extends DriftMetricRequest> extends BaseEndpoint<T> {
    protected DriftEndpoint(String name) {
        super(name);
    }

    // == FUNCTIONS TO BE IMPLEMENTED BY EXTENDORS ======
    // this function must define if the metric values have exceeded the provided threshold
    public abstract MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue);

    // this function should provide a general definition for this class of metric
    public abstract String getGeneralDefinition();

    // this function should provide a specific definition/interpretation of what this specific metric value means
    public abstract String getSpecificDefinition(MetricValueCarrier metricValueCarrier, @ValidDriftMetricRequest T request);

    // this function should provide the functionality of actually calculating a specific metric value for a given request
    @Override
    public abstract MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request);

    // returns the generic definition as a response object
    @Override
    @GET
    @Path("/definition")
    @Operation(summary = "Provide a general definition of this metric.")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefinition() {
        return Response.ok(getGeneralDefinition()).build();
    }

    // defines the request scheduling mechanism
    @POST
    @Operation(summary = "Schedule a recurring computation of this metric.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createRequest(@ValidDriftMetricRequest T request) {
        if (Objects.isNull(request.getThresholdDelta())) {
            final double defaultUpperThresh = metricsConfig.drift().thresholdDelta();
            request.setThresholdDelta(defaultUpperThresh);
        }
        return super.createRequestGeneric(request);
    }

    // == GLOBAL FUNCTIONS ======
    // this function defines the default individual request/response flow, for a single metric calculation at this exact timestamp
    @POST
    @Operation(summary = "Compute the current value of this metric.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response response(@ValidDriftMetricRequest T request) throws DataframeCreateException {

        final Dataframe dataframe;
        try {

            // fill request with default values if none are provided
            if (Objects.isNull(request.getBatchSize())) {
                final int defaultBatchSize = serviceConfig.batchSize().getAsInt();
                LOG.warn("Request batch size is empty. Using the default value of " + defaultBatchSize);
                request.setBatchSize(defaultBatchSize);
            }

            if (Objects.isNull(request.getThresholdDelta())) {
                final double defaultUpperThresh = metricsConfig.drift().thresholdDelta();
                request.setThresholdDelta(defaultUpperThresh);
            }

            // get the entirety of the reference tag
            int totalRows = (int) dataSource.get().getNumObservations(request.getModelId());

            // get the last $batchSize references

            dataframe = dataSource.get().getDataframeFilteredByTags(request.getModelId(), totalRows, Set.of(request.getReferenceTag()));

            try {
                dataframe.addPredictions(
                        dataSource.get().getDataframeFilteredByTags(request.getModelId(), request.getBatchSize(), Set.of(Dataframe.InternalTags.UNLABELED.get())).asPredictions());
            } catch (DataframeCreateException e) {
                // pass: no inference data, which should return a 'null' drift response, i.e., because we are checking if the training data has drifted from itself
            }
        } catch (DataframeCreateException | StorageReadException e) {
            LOG.error(e.getMessage());
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        // use the calculate function to compute the metric value(s)
        final MetricValueCarrier metricValue;
        try {
            metricValue = this.calculate(dataframe, request);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model=" + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error calculating metric").build();
        }

        // get the metric definition and the threshold exceeded state
        final String metricDefinition = this.getSpecificDefinition(metricValue, request);
        MetricThreshold thresholds = thresholdFunction(request.getThresholdDelta(), metricValue);

        // wrap into response
        BaseMetricResponse response = new BaseMetricResponse(metricValue, metricDefinition, thresholds, super.getMetricName());
        return Response.ok(response).build();
    }

    @GET
    @Path("/requests")
    @Operation(summary = "List the currently scheduled computations of this metric.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRequests() {
        final ScheduleList scheduleList = new ScheduleList();
        for (Map.Entry<UUID, BaseMetricRequest> entry : scheduler.getRequests(this.name).entrySet()) {
            scheduleList.requests.add(new ScheduleRequest(entry.getKey(), ((DriftMetricRequest) entry.getValue()).getReadableVersion()));
        }
        return Response.ok(scheduleList).build();
    }
}
