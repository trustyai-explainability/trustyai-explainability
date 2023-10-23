package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.util.Objects;

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
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.fairness.group.ValidGroupMetricRequest;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public abstract class GroupEndpoint extends BaseEndpoint<GroupMetricRequest> {
    protected GroupEndpoint(String name) {
        super(name);
    }

    public abstract MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue);

    public abstract String specificDefinitionFunction(String outcomeName, Value favorableOutcomeAttr, String protectedAttribute, String privileged, String unprivileged,
            MetricValueCarrier metricvalue);

    public abstract String getGeneralDefinition();

    public String getSpecificDefinition(MetricValueCarrier metricValue, @ValidReconciledMetricRequest GroupMetricRequest request) {
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
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("No data available").build();
        }

        RequestReconciler.reconcile(request, metadata);

        final MetricValueCarrier metricValue;
        try {
            metricValue = this.calculate(dataframe, request);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error calculating metric: " + e.getMessage()).build();
        }
        if (metricValue.isSingle()) {
            final String metricDefinition = this.getSpecificDefinition(metricValue, request);

            MetricThreshold thresholds = thresholdFunction(request.getThresholdDelta(), metricValue);
            final BaseMetricResponse dirObj = new BaseMetricResponse(metricValue, metricDefinition, thresholds, super.getMetricName());
            return Response.ok(dirObj).build();
        } else {
            throw new UnsupportedOperationException("Group metric endpoint not yet compatible with multiple-valued metrics");
        }
    }

    @GET
    @Path("/definition")
    @Produces(MediaType.TEXT_PLAIN)
    @Override
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
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("No data available").build();
        }

        return Response.ok(this.getSpecificDefinition(new MetricValueCarrier(request.getMetricValue()), request)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createRequest(@ValidGroupMetricRequest GroupMetricRequest request) {
        return super.createRequestGeneric(request);
    }
}
