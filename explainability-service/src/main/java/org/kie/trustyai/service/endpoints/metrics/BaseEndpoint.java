package org.kie.trustyai.service.endpoints.metrics;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.RequestReconciler;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public abstract class BaseEndpoint<T extends BaseMetricRequest> {
    protected static final Logger LOG = Logger.getLogger(BaseEndpoint.class);

    @Inject
    protected Instance<DataSource> dataSource;

    @Inject
    protected MetricsConfig metricsConfig;

    @Inject
    protected PrometheusScheduler scheduler;

    @Inject
    protected ServiceConfig serviceConfig;

    private final String name;

    protected BaseEndpoint() {
        this.name = "GenericBaseEndpoint";
    }

    protected BaseEndpoint(String name) {
        this.name = name;
    }

    public String getMetricName() {
        return this.name;
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/request")
    public Response deleteRequest(ScheduleId request) {
        final UUID id = request.requestId;
        if (scheduler.getRequests(this.name).containsKey(id)) {
            scheduler.delete(this.name, request.requestId);
            LOG.info("Removing scheduled request id=" + id);
            return RestResponse.ResponseBuilder.ok("Removed").build().toResponse();
        } else {
            LOG.error("Scheduled request id=" + id + " not found");
            return RestResponse.ResponseBuilder.notFound().build().toResponse();
        }
    }

    @GET
    @Path("/requests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRequests() {
        final ScheduleList scheduleList = new ScheduleList();
        for (Map.Entry<UUID, BaseMetricRequest> entry : scheduler.getRequests(this.name).entrySet()) {
            scheduleList.requests.add(new ScheduleRequest(entry.getKey(), entry.getValue()));
        }
        return Response.ok(scheduleList).build();
    }

    // call this after the request type has been validated
    protected Response createRequestGeneric(BaseMetricRequest request) {

        final UUID id = UUID.randomUUID();

        if (Objects.isNull(request.getBatchSize())) {
            final int defaultBatchSize = serviceConfig.batchSize().getAsInt();
            LOG.warn("Request batch size is empty. Using the default value of " + defaultBatchSize);
            request.setBatchSize(defaultBatchSize);
        }
        request.setMetricName(getMetricName());
        scheduler.getMetricsDirectory().register(getMetricName(), this::calculate);

        try {
            RequestReconciler.reconcile(request, dataSource);
        } catch (DataframeCreateException e) {
            LOG.error("No data available: " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("No data available").build();
        }
        scheduler.register(request.getMetricName(), id, request);

        final BaseScheduledResponse response =
                new BaseScheduledResponse(id);

        return Response.ok().entity(response).build();
    }

    public abstract MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request);

    // this function should return a generic definition (as a response) for the corresponding metric
    public abstract Response getDefinition();

}
