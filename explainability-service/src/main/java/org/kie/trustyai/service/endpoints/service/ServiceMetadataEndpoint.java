package org.kie.trustyai.service.endpoints.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.service.*;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.ServiceMetadata;
import org.kie.trustyai.service.payloads.service.readable.ReadableStorageMetadata;
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
@Tag(name = "Service Metadata", description = "The Service Metadata endpoint provides information about TrustyAI, such as details about" +
        "the model inference data it has collected. This endpoint also provides various operations to label and rename the recorded inference data.")
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

    private Map<String, ServiceMetadata> getServiceMetadata() {
        final Map<String, ServiceMetadata> serviceMetadataMap = new HashMap<>();
        for (String modelId : dataSource.get().getKnownModels()) {
            final ServiceMetadata serviceMetadata = new ServiceMetadata();

            for (Map.Entry<String, ConcurrentHashMap<UUID, BaseMetricRequest>> metricDict : scheduler.getAllRequests()
                    .entrySet()) {
                metricDict.getValue().values().forEach(metric -> {
                    if (metric.getModelId().equals(modelId)) {
                        final String metricName = metricDict.getKey();
                        serviceMetadata.getMetrics().scheduledMetadata.setCount(metricName,
                                serviceMetadata.getMetrics().scheduledMetadata.getCount(metricName) + 1);
                    }
                });
            }

            try {
                final StorageMetadata storageMetadata = dataSource.get().getMetadata(modelId);
                serviceMetadata.setData(ReadableStorageMetadata.from(storageMetadata));
            } catch (DataframeCreateException | StorageReadException | NullPointerException e) {
                LOG.warn("Problem creating dataframe: " + e.getMessage(), e);
            }

            serviceMetadataMap.put(modelId, serviceMetadata);
        }
        return serviceMetadataMap;
    }

    @GET
    @Operation(summary = "Get a comprehensive overview of the model inference datasets collected by TrustyAI and the metric computations that are scheduled over those datasets.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response serviceInfo() throws JsonProcessingException {
        return Response.ok(getServiceMetadata()).build();
    }

    @GET
    @Path("/tags")
    @Operation(summary = "Retrieve the tags that have been applied to a particular model dataset, as well as a count of that tag's frequency within the dataset.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTagInformation() {
        Map<String, Map<String, Long>> perModelTagCounts = new HashMap<>();
        for (String modelId : dataSource.get().getVerifiedModels()) {
            List<String> tags = dataSource.get().getTags(modelId);
            Map<String, Long> tagCounts = tags.stream().collect(Collectors.groupingBy(s -> s, Collectors.counting()));
            perModelTagCounts.put(modelId, tagCounts);
        }
        return Response.ok(perModelTagCounts).build();
    }

    @POST
    @Path("/tags")
    @Operation(summary = "Apply per-row tags to a particular inference model dataset, to label certain rows as training or drift reference data, etc.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response labelSchema(DataTagging dataTagging) throws JsonProcessingException {

        if (!dataSource.get().getKnownModels().contains(dataTagging.getModelId())) {
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("No metadata found for model=" + dataTagging.getModelId() + ". This can happen if TrustyAI has not yet logged any inferences from this model.")
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
    @Operation(summary = "Apply a set of human-readable column names to a particular inference model dataset.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response applyNameMappings(@ValidNameMappingRequest NameMapping nameMapping) {

        if (!dataSource.get().getKnownModels().contains(nameMapping.getModelId())) {
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("No metadata found for model=" + nameMapping.getModelId() + ". This can happen if TrustyAI has not yet logged any inferences from this model.")
                    .build();
        }

        dataSource.get().applyNameMapping(nameMapping);

        LOG.info("Name mappings successfully applied to model=" + nameMapping.getModelId() + ".");
        return Response.ok().entity("Feature and output name mapping successfully applied.").build();
    }

    @DELETE
    @Path("/names")
    @Operation(summary = "Remove any column names that have been applied to a particular inference model dataset.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearNameMappings(String modelId) {

        if (!dataSource.get().getKnownModels().contains(modelId)) {
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("No metadata found for model=" + modelId + ". This can happen if TrustyAI has not yet logged any inferences from this model.")
                    .build();
        }

        dataSource.get().clearNameMapping(modelId);

        LOG.info("Name mappings successfully cleared from model=" + modelId + ".");
        return Response.ok().entity("Feature and output name mapping successfully cleared.").build();
    }

    @GET
    @Path("/inference/ids/{model}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a list of all inference ids within a particular model inference dataset.", description = "Get all the inference ids for a given model")
    public Response inferenceIdsByModel(@Parameter(description = "The model to get inference ids from", required = true) @PathParam("model") String model,
            @Parameter(description = "The type of inferences to retrieve", required = false) @QueryParam("type") @DefaultValue("all") String type) {
        final List<InferenceId> ids;

        if (!dataSource.get().getKnownModels().contains(model)) {
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("No metadata found for model=" + model + ". This can happen if TrustyAI has not yet logged any inferences from this model.")
                    .build();
        }

        if ("organic".equalsIgnoreCase(type)) {
            try {
                ids = dataSource.get().getOrganicInferenceIds(model);
            } catch (Exception e) {
                return Response.serverError()
                        .status(Response.Status.BAD_REQUEST)
                        .entity("Error retrieving organic inferences for model=" + model)
                        .build();
            }
        } else if ("all".equalsIgnoreCase(type)) {
            ids = dataSource.get().getAllInferenceIds(model);
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid type parameter. Valid values must be in ['organic', 'all'].")
                    .build();
        }
        return Response.ok().entity(ids).build();

    }

}
