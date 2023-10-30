package org.kie.trustyai.service.endpoints.metrics;

import java.util.Map;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Metrics Information Endpoint", description = "Return a list of all metrics being currently requested.")
@Path("/metrics/all")
public class UniversalListingEndpoint {
    private static final Logger LOG = Logger.getLogger(UniversalListingEndpoint.class);

    @Inject
    PrometheusScheduler scheduler;

    UniversalListingEndpoint() {
        super();
    }

    @GET
    @Path("/requests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRequests() {
        final ScheduleList scheduleList = new ScheduleList();
        for (Map.Entry<UUID, BaseMetricRequest> entry : scheduler.getAllRequestsFlat().entrySet()) {
            scheduleList.requests.add(new ScheduleRequest(entry.getKey(), entry.getValue()));
        }
        return Response.ok(scheduleList).build();
    }
}
