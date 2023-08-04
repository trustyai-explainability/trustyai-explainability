package org.kie.trustyai.service.endpoints.metrics;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.model.Dataframe;
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
import org.kie.trustyai.service.prometheus.PrometheusScheduler;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

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
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity("No data available").build();
        }
        scheduler.register(request.getMetricName(), id, request);

        final BaseScheduledResponse response =
                new BaseScheduledResponse(id);

        return Response.ok().entity(response).build();
    }

    public abstract double calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request);
}
