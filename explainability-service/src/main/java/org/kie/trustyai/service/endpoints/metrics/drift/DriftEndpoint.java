package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.endpoints.metrics.BaseEndpoint;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.drift.ValidDriftMetricRequest;

public abstract class DriftEndpoint extends BaseEndpoint<DriftMetricRequest> {
    protected DriftEndpoint(String name) {
        super(name);
    }

    // == FUNCTIONS TO BE IMPLEMENTED BY EXTENDORS ======
    // this function must define if the metric values have exceeded the provided threshold
    public abstract MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue);

    // this function should provide a general definition for this class of metric
    public abstract String getGeneralDefinition();

    // this function should provide a specific definition/interpretation of what this specific metric value means
    public abstract String getSpecificDefinition(MetricValueCarrier metricValueCarrier, @ValidDriftMetricRequest DriftMetricRequest request);

    // this function should provide the functionality of actually calculating a specific metric value for a given request
    @Override
    public abstract MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request);

    // returns the generic definition as a response object
    @Override
    @GET
    @Path("/definition")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefinition() {
        return Response.ok(getGeneralDefinition()).build();
    }

    // defines the request scheduling mechanism
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createRequest(@ValidDriftMetricRequest DriftMetricRequest request) {
        if (Objects.isNull(request.getThresholdDelta())) {
            final double defaultUpperThresh = metricsConfig.drift().thresholdDelta();
            request.setThresholdDelta(defaultUpperThresh);
        }
        return super.createRequestGeneric(request);
    }

    // == GLOBAL FUNCTIONS ======
    // this function defines the default individual request/response flow, for a single metric calculation at this exact timestamp
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response response(@ValidDriftMetricRequest DriftMetricRequest request) throws DataframeCreateException {

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

            // grab the slice of the requested data according to the provided batch size
            dataframe = super.dataSource.get().getDataframe(request.getModelId(), request.getBatchSize()).filterRowsBySynthetic(false);
        } catch (DataframeCreateException e) {
            LOG.error("No data available for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        // use the calculate function to compute the metric value(s)
        final MetricValueCarrier metricValue;
        try {
            metricValue = this.calculate(dataframe, request);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("Error calculating metric").build();
        }

        // get the metric definition and the threshold exceeded state
        final String metricDefinition = this.getSpecificDefinition(metricValue, request);
        MetricThreshold thresholds = thresholdFunction(request.getThresholdDelta(), metricValue);

        // wrap into response
        BaseMetricResponse response = new BaseMetricResponse(metricValue, metricDefinition, thresholds, super.getMetricName());
        return Response.ok(response).build();
    }
}
