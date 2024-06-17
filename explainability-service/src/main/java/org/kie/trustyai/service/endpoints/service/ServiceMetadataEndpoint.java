package org.kie.trustyai.service.endpoints.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;
import org.kie.trustyai.service.prometheus.PrometheusScheduler;
import org.kie.trustyai.service.validators.generic.GenericValidationUtils;
import org.kie.trustyai.service.validators.serviceRequests.ValidNameMappingRequest;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
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

            for (Map.Entry<String, ConcurrentHashMap<UUID, BaseMetricRequest>> metricDict : scheduler.getAllRequests().entrySet()) {
                metricDict.getValue().values().forEach(metric -> {
                    if (metric.getModelId().equals(modelId)) {
                        final String metricName = metricDict.getKey();
                        serviceMetadata.getMetrics().scheduledMetadata.setCount(metricName, serviceMetadata.getMetrics().scheduledMetadata.getCount(metricName) + 1);
                    }
                });
            }

            try {
                final StorageMetadata storageMetadata = dataSource.get().getMetadata(modelId, true);
                serviceMetadata.setData(storageMetadata);
            } catch (DataframeCreateException | StorageReadException | NullPointerException e) {
                LOG.warn("Problem creating dataframe: " + e.getMessage(), e);
            }

            serviceMetadataList.add(serviceMetadata);

        }

        return Response.ok(serviceMetadataList).build();

    }

    @GET
    @Path("/tags")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagInformation() {
        Map<String, Map<String, Long>> perModelTagCounts = new HashMap<>();
        for (String modelId : dataSource.get().getKnownModels()) {
            List<String> tags = dataSource.get().getTags(modelId);
            Map<String, Long> tagCounts = tags.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            perModelTagCounts.put(modelId, tagCounts);
        }
        return Response.ok(perModelTagCounts).build();
    }

    @POST
    @Path("/tags")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response labelSchema(DataTagging dataTagging) throws JsonProcessingException {

        if (!dataSource.get().getKnownModels().contains(dataTagging.getModelId())) {
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Model ID " + dataTagging.getModelId() + " does not exist in TrustyAI metadata.")
                    .build();
        }

        try {
            HashMap<String, List<List<Integer>>> tagMapping = new HashMap<>();
            List<String> tagErrors = new ArrayList<>();
            for (String tag : dataTagging.getDataTagging().keySet()) {
                Optional<String> tagValidationErrorMessage = GenericValidationUtils.validateDataTag(tag);
                tagValidationErrorMessage.ifPresent(tagErrors::add);
                tagMapping.put(tag, dataTagging.getDataTagging().get(tag));
            }

            if (!tagErrors.isEmpty()) {
                return Response.serverError().entity(String.join(", ", tagErrors)).status(Response.Status.BAD_REQUEST).build();
            }

            dataSource.get().tagDataframeRows(dataTagging);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity(e.getMessage())
                    .build();
        }

        return Response.ok().entity("Datapoints successfully tagged.").build();
    }

    @POST
    @Path("/names")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response labelSchema(@ValidNameMappingRequest NameMapping nameMapping) {

        if (!dataSource.get().getKnownModels().contains(nameMapping.getModelId())) {
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Model ID " + nameMapping.getModelId() + " does not exist in TrustyAI metadata.")
                    .build();
        }

        dataSource.get().applyNameMapping(nameMapping);

        LOG.info("Name mappings successfully applied to model=" + nameMapping.getModelId() + ".");
        return Response.ok().entity("Feature and output name mapping successfully applied.").build();
    }

}
