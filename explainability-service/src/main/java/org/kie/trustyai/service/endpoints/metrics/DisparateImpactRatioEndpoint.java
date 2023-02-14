package org.kie.trustyai.service.endpoints.metrics;

import java.util.UUID;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.RestResponse;
import org.kie.trustyai.explainability.metrics.utils.FairnessDefinitions;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.BaseMetricRequest;
import org.kie.trustyai.service.payloads.BaseScheduledResponse;
import org.kie.trustyai.service.payloads.MetricThreshold;
import org.kie.trustyai.service.payloads.definitions.DefinitionRequest;
import org.kie.trustyai.service.payloads.dir.DisparateImpactRatioResponse;
import org.kie.trustyai.service.payloads.scheduler.ScheduleId;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Tag(name = "Disparate Impact Ratio Endpoint", description = "Disparate Impact Ratio (DIR) measures imbalances in " +
        "classifications by calculating the ratio between the proportion of the majority and protected classes getting" +
        " a particular outcome.")
@Path("/metrics/dir")
public class DisparateImpactRatioEndpoint extends AbstractMetricsEndpoint {

    private static final Logger LOG = Logger.getLogger(DisparateImpactRatioEndpoint.class);
    @Inject
    DataSource dataSource;

    @Inject
    MetricsConfig metricsConfig;

    @Inject
    MetricsCalculator calculator;

    @Inject
    PrometheusScheduler scheduler;

    DisparateImpactRatioEndpoint() {
        super();
    }

    @Override
    public String getMetricName() {
        return "dir";
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response dir(BaseMetricRequest request) throws DataframeCreateException {

        final Dataframe df = dataSource.getDataframe();

        final double dir = calculator.calculateDIR(df, request);
        final String dirDefinition = calculator.getDIRDefinition(dir, request);

        final MetricThreshold thresholds =
                new MetricThreshold(metricsConfig.dir().thresholdLower(), metricsConfig.dir().thresholdUpper(), dir);
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
        return Response.ok(calculator.getDIRDefinition(request.getMetricValue(), request)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public String createRequest(BaseMetricRequest request) throws JsonProcessingException {

        final UUID id = UUID.randomUUID();

        scheduler.registerDIR(id, request);

        final BaseScheduledResponse response =
                new BaseScheduledResponse(id);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(response);
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

}
