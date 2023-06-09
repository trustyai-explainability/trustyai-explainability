package org.kie.trustyai.service.endpoints.metrics;

import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.payloads.ReconciledMetricRequest;
import org.kie.trustyai.service.payloads.scheduler.ScheduleList;
import org.kie.trustyai.service.payloads.scheduler.ScheduleRequest;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;

@Tag(name = "Metrics Information Endpoint", description = "Return a list of all metrics being currently requested.")
@Path("/metrics/all")
public class UniversalListingEndpoint implements MetricsEndpoint {
    private static final Logger LOG = Logger.getLogger(UniversalListingEndpoint.class);

    @Inject
    PrometheusScheduler scheduler;

    UniversalListingEndpoint() {
        super();
    }

    @Override
    public String getMetricName() {
        return "spd";
    }

    @Override
    @GET
    @Path("/requests")
    @Produces(MediaType.APPLICATION_JSON)
    public Response listRequests() {
        final ScheduleList scheduleList = new ScheduleList();
        for (Map.Entry<UUID, ReconciledMetricRequest> entry : scheduler.getAllRequests().entrySet()) {
            scheduleList.requests.add(new ScheduleRequest(entry.getKey(), entry.getValue()));
        }
        return Response.ok(scheduleList).build();
    }
}
