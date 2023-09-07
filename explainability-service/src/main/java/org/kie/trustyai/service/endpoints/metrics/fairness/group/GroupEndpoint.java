package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.endpoints.metrics.BaseEndpoint;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.definitions.GroupDefinitionRequest;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.RequestReconciler;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.fairness.group.ValidGroupMetricRequest;

public abstract class GroupEndpoint extends BaseEndpoint<GroupMetricRequest> {
    protected GroupEndpoint(String name) {
        super(name);
    }

    public abstract MetricThreshold thresholdFunction(Number delta, Number metricValue);

    public abstract String specificDefinitionFunction(String outcomeName, Value favorableOutcomeAttr, String protectedAttribute, String privileged, String unprivileged, Number metricvalue);

    public abstract String getGeneralDefinition();

    public String getSpecificDefinition(Number metricValue, @ValidReconciledMetricRequest GroupMetricRequest request) {
        final String outcomeName = request.getOutcomeName();

        PayloadConverter.convertToValue(request.getFavorableOutcome().getReconciledType().get());
        final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome().getReconciledType().get());
        final String protectedAttribute = request.getProtectedAttribute();
        final String privileged = PayloadConverter.convertToValue(request.getPrivilegedAttribute().getReconciledType().get()).toString();
        final String unprivileged = PayloadConverter.convertToValue(request.getUnprivilegedAttribute().getReconciledType().get()).toString();
        return specificDefinitionFunction(outcomeName, favorableOutcomeAttr, protectedAttribute, privileged, unprivileged, metricValue);
    };

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response response(@ValidGroupMetricRequest GroupMetricRequest request) throws DataframeCreateException {

        final Dataframe dataframe;
        final Metadata metadata;
        try {
            if (Objects.isNull(request.getBatchSize())) {
                final int defaultBatchSize = serviceConfig.batchSize().getAsInt();
                LOG.warn("Request batch size is empty. Using the default value of " + defaultBatchSize);
                request.setBatchSize(defaultBatchSize);
            }
            dataframe = super.dataSource.get().getDataframe(request.getModelId(), request.getBatchSize()).filterRowsBySynthetic(false);
            metadata = dataSource.get().getMetadata(request.getModelId());
        } catch (DataframeCreateException e) {
            LOG.error("No data available for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        RequestReconciler.reconcile(request, metadata);

        final double metricValue;
        try {
            metricValue = this.calculate(dataframe, request);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("Error calculating metric").build();
        }
        final String metricDefinition = this.getSpecificDefinition(metricValue, request);

        MetricThreshold thresholds = thresholdFunction(request.getThresholdDelta(), metricValue);
        final BaseMetricResponse dirObj = new BaseMetricResponse(metricValue, metricDefinition, thresholds, super.getMetricName());
        return Response.ok(dirObj).build();
    }

    @GET
    @Path("/definition")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefinition() {
        return Response.ok(getGeneralDefinition()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/definition")
    public Response getSpecificDefinition(GroupDefinitionRequest request) {
        try {
            RequestReconciler.reconcile(request, dataSource);
        } catch (DataframeCreateException e) {
            LOG.error("No data available: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        return Response.ok(this.getSpecificDefinition(request.getMetricValue(), request)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createRequest(@ValidGroupMetricRequest GroupMetricRequest request) {
        return super.createRequestGeneric(request);
    }
}
