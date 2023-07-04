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
import org.kie.trustyai.metrics.fairness.group.DisparateImpactRatio;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpoint;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;
import org.kie.trustyai.service.payloads.definitions.GroupDefinitionRequest;
import org.kie.trustyai.service.payloads.definitions.ReconciledGroupDefinitionRequest;
import org.kie.trustyai.service.payloads.dir.DisparateImpactRatioResponseGroup;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;
import org.kie.trustyai.service.validators.metrics.fairness.group.ValidBaseMetricRequest;

@Tag(name = "Disparate Impact Ratio Endpoint", description = "Disparate Impact Ratio (DIR) measures imbalances in " +
        "classifications by calculating the ratio between the proportion of the majority and protected classes getting" +
        " a particular outcome.")
@Path("/metrics/dir")
public class DisparateImpactRatioEndpoint implements MetricsEndpoint {

    private static final Logger LOG = Logger.getLogger(DisparateImpactRatioEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    MetricsConfig metricsConfig;


    @Inject
    PrometheusScheduler scheduler;

    @Inject
    ServiceConfig serviceConfig;

    DisparateImpactRatioEndpoint() {
        super();
    }

    @Override
    public String getMetricName() {
        return "DIR";
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response dir(@ValidBaseMetricRequest GroupMetricRequest rawRequest) throws DataframeCreateException {

        final Dataframe dataframe;
        final Metadata metadata;
        try {
            dataframe = dataSource.get().getDataframe(rawRequest.getModelId());
            metadata = dataSource.get().getMetadata(rawRequest.getModelId());
        } catch (DataframeCreateException e) {
            LOG.error("No data available for model " + rawRequest.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        ReconciledGroupMetricRequest reconciledMetricRequest = ReconciledGroupMetricRequest.reconcile(rawRequest, metadata);

        final double dir;
        try {
            dir = this.calculate(dataframe, reconciledMetricRequest);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model " + reconciledMetricRequest.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("Error calculating metric").build();
        }
        final String dirDefinition = this.getDefinition(dir, reconciledMetricRequest);

        MetricThreshold thresholds;
        if (reconciledMetricRequest.getThresholdDelta() == null) {
            thresholds =
                    new MetricThreshold(
                            metricsConfig.dir().thresholdLower(),
                            metricsConfig.dir().thresholdUpper(),
                            dir);
        } else {
            thresholds =
                    new MetricThreshold(
                            1 - reconciledMetricRequest.getThresholdDelta(),
                            1 + reconciledMetricRequest.getThresholdDelta(),
                            dir);
        }
        final DisparateImpactRatioResponseGroup dirObj = new DisparateImpactRatioResponseGroup(dir, dirDefinition, thresholds);
        return Response.ok(dirObj).build();
    }

    @GET
    @Path("/definition")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefinition() {
        return Response.ok(FairnessDefinitions.defineGroupDisparateImpactRatio()).build();
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
    public Response createRequest(@ValidBaseMetricRequest GroupMetricRequest request) {

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
        scheduler.registerDIR(id, reconciledMetricRequest);

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

        if (scheduler.getDirRequests().containsKey(id)) {
            scheduler.getDirRequests().remove(request.requestId);
            LOG.info("Removing scheduled request id=" + id);
            return RestResponse.ResponseBuilder.ok("Removed").build().toResponse();
        } else {
            LOG.error("Scheduled request id=" + id + " not found");
            return RestResponse.ResponseBuilder.notFound().build().toResponse();
        }
    }

    @Override
    @GET
    @Path("/requests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRequests() {
        final ScheduleList scheduleList = new ScheduleList();
        for (Map.Entry<UUID, ReconciledGroupMetricRequest> entry : scheduler.getDirRequests().entrySet()) {
            scheduleList.requests.add(new ScheduleRequest(entry.getKey(), entry.getValue()));
        }
        return Response.ok(scheduleList).build();
    }

    @CacheResult(cacheName = "metrics-calculator", keyGenerator = MetricCalculationCacheKeyGen.class)
    public double calculate(Dataframe dataframe, ReconciledGroupMetricRequest request) {
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
            return DisparateImpactRatio.calculate(privileged, unprivileged,
                    List.of(new Output(request.getOutcomeName(), favorableOutcomeAttrType, favorableOutcomeAttr, 1.0)));
        } catch (Exception e) {
            throw new MetricCalculationException(e.getMessage(), e);
        }
    }


    public String getDefinition(double referenceValue, ReconciledGroupMetricRequest request) {
        final String outcomeName = request.getOutcomeName();
        final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
        final String protectedAttribute = request.getProtectedAttribute();
        final String priviliged = PayloadConverter.convertToValue(request.getPrivilegedAttribute()).toString();
        final String unpriviliged = PayloadConverter.convertToValue(request.getUnprivilegedAttribute()).toString();
        return FairnessDefinitions.defineGroupDisparateImpactRatio(
                protectedAttribute,
                priviliged,
                unpriviliged,
                outcomeName,
                favorableOutcomeAttr,
                referenceValue);
    }
}
