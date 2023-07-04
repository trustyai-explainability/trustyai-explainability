package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.quarkus.cache.CacheResult;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.fairness.FairnessDefinitions;
import org.kie.trustyai.metrics.fairness.group.GroupStatisticalParityDifference;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpoint;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.metrics.ReconciledBaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;
import org.kie.trustyai.service.payloads.definitions.GroupDefinitionRequest;
import org.kie.trustyai.service.payloads.definitions.ReconciledGroupDefinitionRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceResponseGroup;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;
import org.kie.trustyai.service.validators.metrics.fairness.group.ValidBaseMetricRequest;

@Tag(name = "Statistical Parity Difference Endpoint", description = "Statistical Parity Difference (SPD) measures imbalances in classifications by calculating the " +
        "difference between the proportion of the majority and protected classes getting a particular outcome.")
@Path("/metrics/spd")
public class GroupStatisticalParityDifferenceEndpoint implements MetricsEndpoint {

    private static final Logger LOG = Logger.getLogger(GroupStatisticalParityDifferenceEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    PrometheusScheduler scheduler;

    @Inject
    MetricsConfig metricsConfig;

    @Inject
    ServiceConfig serviceConfig;

    GroupStatisticalParityDifferenceEndpoint() {
        super();
    }

    @Override
    public String getMetricName() {
        return "SPD";
    }

    @Override
    @GET
    @Path("/requests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRequests() {
        final ScheduleList scheduleList = new ScheduleList();
        for (Map.Entry<UUID, ReconciledGroupMetricRequest> entry : scheduler.getSpdRequests().entrySet()) {
            scheduleList.requests.add(new ScheduleRequest(entry.getKey(), entry.getValue()));
        }
        return Response.ok(scheduleList).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response spd(@ValidBaseMetricRequest GroupMetricRequest request) throws DataframeCreateException {

        final Dataframe dataframe;
        final Metadata metadata;
        try {
            dataframe = dataSource.get().getDataframe(request.getModelId());
            metadata = dataSource.get().getMetadata(request.getModelId());
        } catch (DataframeCreateException e) {
            LOG.error("No data available: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        ReconciledGroupMetricRequest reconciledMetricRequest = ReconciledGroupMetricRequest.reconcile(request, metadata);

        final double spd;
        try {
            spd = this.calculate(dataframe, reconciledMetricRequest);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("Error calculating metric").build();
        }
        final String definition = this.getDefinition(spd, reconciledMetricRequest);

        MetricThreshold thresholds;
        if (request.getThresholdDelta() == null) {
            thresholds =
                    new MetricThreshold(
                            metricsConfig.spd().thresholdLower(),
                            metricsConfig.spd().thresholdUpper(),
                            spd);
        } else {
            thresholds =
                    new MetricThreshold(
                            0 - request.getThresholdDelta(),
                            request.getThresholdDelta(),
                            spd);
        }
        final GroupStatisticalParityDifferenceResponseGroup spdObj = new GroupStatisticalParityDifferenceResponseGroup(spd, definition, thresholds);
        return Response.ok(spdObj).build();
    }

    @GET
    @Path("/definition")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefinition() {
        return Response.ok(FairnessDefinitions.defineGroupStatisticalParityDifference()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/definition")
    public Response getSpecificDefinition(GroupDefinitionRequest request) {
        final ReconciledGroupMetricRequest reconciledMetricRequest;
        try {
            reconciledMetricRequest = ReconciledGroupMetricRequest.reconcile(request, dataSource);
        } catch (DataframeCreateException e) {
            LOG.error("No data available: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        ReconciledGroupDefinitionRequest reconciledDefinitionRequest = new ReconciledGroupDefinitionRequest(reconciledMetricRequest, request.metricValue);
        return Response.ok(this.getDefinition(request.getMetricValue(), reconciledDefinitionRequest)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createaRequest(@ValidBaseMetricRequest GroupMetricRequest request) {
        final UUID id = UUID.randomUUID();

        if (Objects.isNull(request.getBatchSize())) {
            final int defaultBatchSize = serviceConfig.batchSize().getAsInt();
            LOG.warn("Request batch size is empty. Using the default value of " + defaultBatchSize);
            request.setBatchSize(defaultBatchSize);
        }

        request.setMetricName(getMetricName());

        final ReconciledGroupMetricRequest reconciledMetricRequest;
        try {
            reconciledMetricRequest = ReconciledGroupMetricRequest.reconcile(request, dataSource);
        } catch (DataframeCreateException e) {
            LOG.error("No data available: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }
        scheduler.registerSPD(id, reconciledMetricRequest);

        final BaseScheduledResponse response =
                new BaseScheduledResponse(id);

        return Response.ok().entity(response).build();
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/request")
    public Response deleteRequest(ScheduleId request) {

        final UUID id = request.requestId;

        if (scheduler.getSpdRequests().containsKey(id)) {
            scheduler.getSpdRequests().remove(request.requestId);
            LOG.info("Removing scheduled request id=" + id);
            return RestResponse.ResponseBuilder.ok("Removed").build().toResponse();
        } else {
            LOG.error("Scheduled request id=" + id + " not found");
            return RestResponse.ResponseBuilder.notFound().build().toResponse();
        }
    }

    @Override
    @CacheResult(cacheName = "metrics-calculator", keyGenerator = MetricCalculationCacheKeyGen.class)
    public double calculate(Dataframe dataframe, ReconciledGroupMetricRequest request) throws MetricCalculationException {

        LOG.debug("Cache miss. Calculating metric for " + request.getModelId());
        try {
            final int protectedIndex = dataframe.getColumnNames().indexOf(request.getProtectedAttribute());
            final Value privilegedAttr = PayloadConverter.convertToValue(request.getPrivilegedAttribute());
            final Dataframe privileged = dataframe.filterByColumnValue(protectedIndex,
                    value -> value.equals(privilegedAttr));
            final Value unprivilegedAttr = PayloadConverter.convertToValue(request.getUnprivilegedAttribute());
            final Dataframe unprivileged = dataframe.filterByColumnValue(protectedIndex,
                    value -> value.equals(unprivilegedAttr));
            final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
            final Type favorableOutcomeAttrType = PayloadConverter.convertToType(request.getFavorableOutcome().getType());

            return GroupStatisticalParityDifference.calculate(privileged, unprivileged,
                    List.of(new Output(request.getOutcomeName(), favorableOutcomeAttrType, favorableOutcomeAttr, 1.0)));
        } catch (Exception e) {
            throw new MetricCalculationException(e.getMessage(), e);
        }
    }

    @Override
    public String getDefinition(double referenceValue, ReconciledGroupMetricRequest request) {
        final String outcomeName = request.getOutcomeName();
        final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
        final String protectedAttribute = request.getProtectedAttribute();
        final String priviliged = PayloadConverter.convertToValue(request.getPrivilegedAttribute()).toString();
        final String unpriviliged = PayloadConverter.convertToValue(request.getUnprivilegedAttribute()).toString();

        return FairnessDefinitions.defineGroupStatisticalParityDifference(
                protectedAttribute,
                priviliged,
                unpriviliged,
                outcomeName,
                favorableOutcomeAttr,
                referenceValue);
    }

}
