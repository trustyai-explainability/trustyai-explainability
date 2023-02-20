package org.kie.trustyai.service.endpoints.service;

import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.service.SchemaItem;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;

@Path("/info")
public class ServiceMetadataEndpoint {

    private static final Logger LOG = Logger.getLogger(ServiceMetadataEndpoint.class);
    @Inject
    DataSource dataSource;

    @Inject
    PrometheusScheduler scheduler;

    @Inject
    MetricsConfig metricsConfig;

    ServiceMetadataEndpoint() {

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response serviceInfo() {
        final ServiceMetadata metadata = new ServiceMetadata();

        metadata.metrics.scheduledMetadata.dir = scheduler.getDirRequests().size();
        metadata.metrics.scheduledMetadata.spd = scheduler.getSpdRequests().size();

        try {
            final Dataframe dataframe = dataSource.getDataframe();
            final int observations = dataframe.getRowDimension();
            if (observations > 0) {

                Function<Integer, SchemaItem> extractRowSchema = i -> {
                    final Value value = dataframe.getValue(0, i);
                    final SchemaItem schemaItem = new SchemaItem();
                    if (value.getUnderlyingObject() instanceof Integer) {
                        schemaItem.type = "INT32";
                    } else if (value.getUnderlyingObject() instanceof Double) {
                        schemaItem.type = "DOUBLE";
                    } else if (value.getUnderlyingObject() instanceof Long) {
                        schemaItem.type = "INT64";
                    } else if (value.getUnderlyingObject() instanceof Boolean) {
                        schemaItem.type = "BOOL";
                    } else if (value.getUnderlyingObject() instanceof String) {
                        schemaItem.type = "STRING";
                    }
                    schemaItem.name = dataframe.getColumnNames().get(i);
                    return schemaItem;
                };

                metadata.data.inputs = dataframe.getInputsIndices().stream().map(extractRowSchema).collect(Collectors.toList());
                metadata.data.outputs = dataframe.getOutputsIndices().stream().map(extractRowSchema).collect(Collectors.toList());

            }

        } catch (DataframeCreateException e) {
            LOG.warn("Problem creating dataframe: " + e.getMessage(), e);
        }
        return Response.ok(metadata).build();

    }

}
