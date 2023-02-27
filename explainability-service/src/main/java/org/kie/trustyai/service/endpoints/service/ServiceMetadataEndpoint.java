package org.kie.trustyai.service.endpoints.service;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;

import com.fasterxml.jackson.core.JsonProcessingException;

@Path("/info")
public class ServiceMetadataEndpoint {

    private static final Logger LOG = Logger.getLogger(ServiceMetadataEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    PrometheusScheduler scheduler;

    @Inject
    MetricsConfig metricsConfig;

    ServiceMetadataEndpoint() {

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response serviceInfo() throws JsonProcessingException {

        final List<ServiceMetadata> serviceMetadataList = new ArrayList<>();

        for (String modelId : dataSource.get().getKnownModels()) {
            final ServiceMetadata serviceMetadata = new ServiceMetadata();

            serviceMetadata.getMetrics().scheduledMetadata.dir = scheduler.getDirRequests().size();
            serviceMetadata.getMetrics().scheduledMetadata.spd = scheduler.getSpdRequests().size();

            try {
                final Metadata metadata = dataSource.get().getMetadata(modelId);
                serviceMetadata.setData(metadata);
            } catch (DataframeCreateException | StorageReadException | NullPointerException e) {
                LOG.warn("Problem creating dataframe: " + e.getMessage(), e);
            }

            serviceMetadataList.add(serviceMetadata);

        }

        return Response.ok(serviceMetadataList).build();

    }

}
