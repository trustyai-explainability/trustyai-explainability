package org.kie.trustyai.service.endpoints.metrics;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.metrics.fairness.FairnessDefinitions;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.BaseMetricRequest;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.MetricThreshold;
import org.kie.trustyai.service.payloads.ReconciledMetricRequest;
import org.kie.trustyai.service.payloads.definitions.DefinitionRequest;
import org.kie.trustyai.service.payloads.definitions.ReconciledDefinitionRequest;
import org.kie.trustyai.service.payloads.dir.DisparateImpactRatioResponse;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;
import org.kie.trustyai.service.validators.ValidBaseMetricRequest;

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
    MetricsCalculator calculator;

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
    public Response dir(@ValidBaseMetricRequest BaseMetricRequest rawRequest) throws DataframeCreateException {

        final Dataframe dataframe;
        final Metadata metadata;
        try {
            dataframe = dataSource.get().getDataframe(rawRequest.getModelId());
            metadata = dataSource.get().getMetadata(rawRequest.getModelId());
        } catch (DataframeCreateException e) {
            LOG.error("No data available for model " + rawRequest.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        ReconciledMetricRequest reconciledMetricRequest = ReconciledMetricRequest.reconcile(rawRequest, metadata);

        final double dir;
        try {
            dir = calculator.calculateDIR(dataframe, reconciledMetricRequest);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model " + reconciledMetricRequest.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("Error calculating metric").build();
        }
        final String dirDefinition = calculator.getDIRDefinition(dir, reconciledMetricRequest);

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
        final DisparateImpactRatioResponse dirObj = new DisparateImpactRatioResponse(dir, dirDefinition, thresholds);
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
    public Response getSpecificDefinition(DefinitionRequest request) {
        final ReconciledMetricRequest reconciledMetricRequest;
        try {
            reconciledMetricRequest = ReconciledMetricRequest.reconcile(request, dataSource);
        } catch (DataframeCreateException e) {
            LOG.error("No data available: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        ReconciledDefinitionRequest reconciledDefinitionRequest = new ReconciledDefinitionRequest(reconciledMetricRequest, request.metricValue);
        return Response.ok(calculator.getDIRDefinition(request.getMetricValue(), reconciledDefinitionRequest)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createRequest(@ValidBaseMetricRequest BaseMetricRequest request) {

        final UUID id = UUID.randomUUID();

        if (Objects.isNull(request.getBatchSize())) {
            final int defaultBatchSize = serviceConfig.batchSize().getAsInt();
            LOG.warn("Request batch size is empty. Using the default value of " + defaultBatchSize);
            request.setBatchSize(defaultBatchSize);
        }
        request.setMetricName(getMetricName());

        final ReconciledMetricRequest reconciledMetricRequest;
        try {
            reconciledMetricRequest = ReconciledMetricRequest.reconcile(request, dataSource);
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
        for (Map.Entry<UUID, ReconciledMetricRequest> entry : scheduler.getDirRequests().entrySet()) {
            scheduleList.requests.add(new ScheduleRequest(entry.getKey(), entry.getValue()));
        }
        return Response.ok(scheduleList).build();
    }
}
