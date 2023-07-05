package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.endpoints.metrics.MetricsEndpoint;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.definitions.GroupDefinitionRequest;
import org.kie.trustyai.service.payloads.definitions.ReconciledGroupDefinitionRequest;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.ReconciledBaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;
import org.kie.trustyai.service.validators.metrics.fairness.group.ValidGroupBaseMetricRequest;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Tag(name = "Disparate Impact Ratio Endpoint", description = "Disparate Impact Ratio (DIR) measures imbalances in " +
        "classifications by calculating the ratio between the proportion of the majority and protected classes getting" +
        " a particular outcome.")
public abstract class GroupEndpoint implements MetricsEndpoint {

    protected static final Logger LOG = Logger.getLogger(GroupEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    MetricsConfig metricsConfig;


    @Inject
    PrometheusScheduler scheduler;

    @Inject
    ServiceConfig serviceConfig;


    final String name;

    GroupEndpoint(String name) {this.name = name; }

    @Override
    public String getMetricName() {
        return this.name;
    }

    public abstract MetricThreshold thresholdFunction(Number delta, Number metricValue);

    public abstract Number calculate(Dataframe dataframe, ReconciledGroupMetricRequest request);

    public abstract String specificDefinitionFunction(String outcomeName, Value favorableOutcomeAttr, String protectedAttribute, String privileged, String unprivileged, Number metricvalue);

    public abstract String getGeneralDefinition();

    public String getSpecificDefinition(Number metricValue, ReconciledGroupMetricRequest request) {
        final String outcomeName = request.getOutcomeName();
        final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
        final String protectedAttribute = request.getProtectedAttribute();
        final String privileged = PayloadConverter.convertToValue(request.getPrivilegedAttribute()).toString();
        final String unprivileged = PayloadConverter.convertToValue(request.getUnprivilegedAttribute()).toString();
        return specificDefinitionFunction(outcomeName, favorableOutcomeAttr, protectedAttribute, privileged, unprivileged, metricValue);
    };


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response response(@ValidGroupBaseMetricRequest GroupMetricRequest rawRequest) throws DataframeCreateException {

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

        final double metricValue;
        try {
            metricValue = this.calculate(dataframe, reconciledMetricRequest).doubleValue();
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model " + reconciledMetricRequest.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("Error calculating metric").build();
        }
        final String metricDefinition = this.getSpecificDefinition(metricValue, reconciledMetricRequest);

        MetricThreshold thresholds = thresholdFunction(reconciledMetricRequest.getThresholdDelta(), metricValue);
        final BaseMetricResponse dirObj = new BaseMetricResponse(metricValue, metricDefinition, thresholds, name.toLowerCase());
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
        final ReconciledGroupMetricRequest reconciledMetricRequest;
        try {
            reconciledMetricRequest = ReconciledGroupMetricRequest.reconcile(request, dataSource);
        } catch (DataframeCreateException e) {
            LOG.error("No data available: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }

        ReconciledGroupDefinitionRequest reconciledDefinitionRequest = new ReconciledGroupDefinitionRequest(reconciledMetricRequest, request.metricValue);
        return Response.ok(this.getSpecificDefinition(request.getMetricValue(), reconciledDefinitionRequest)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createRequest(@ValidGroupBaseMetricRequest GroupMetricRequest request) {

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
        scheduler.register(reconciledMetricRequest.getMetricName(), id, reconciledMetricRequest);

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

        if (scheduler.getRequests(this.name).containsKey(id)) {
            scheduler.getRequests(this.name).remove(request.requestId);
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
        for (Map.Entry<UUID, ReconciledBaseMetricRequest> entry : scheduler.getRequests(this.name).entrySet()) {
            scheduleList.requests.add(new ScheduleRequest(entry.getKey(), entry.getValue()));
        }
        return Response.ok(scheduleList).build();
    }
}
