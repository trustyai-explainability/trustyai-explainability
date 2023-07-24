package org.kie.trustyai.service.endpoints.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.metrics.MetricsConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.Schema;
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

            for (Map.Entry<String, ConcurrentHashMap<UUID, BaseMetricRequest>> metricDict : scheduler.getAllRequests().entrySet()) {
                serviceMetadata.getMetrics().scheduledMetadata.setCount(metricDict.getKey(), metricDict.getValue().size());
            }

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

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response labelSchema(NameMapping nameMapping) throws JsonProcessingException {

        if (!dataSource.get().getKnownModels().contains(nameMapping.getModelId())) {
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Model ID " + nameMapping.getModelId() + " does not exist in TrustyAI metadata.")
                    .build();
        }
        final Metadata metadata = dataSource.get().getMetadata(nameMapping.getModelId());

        // validation
        Schema inputSchema = metadata.getInputSchema();
        System.out.println("LABEL SCHEMA NM: "+nameMapping.getInputMapping());
        Set<String> inputKeySet = inputSchema.getItems().keySet();
        Set<String> nameMappingInputKeySet = nameMapping.getInputMapping().keySet();

        Schema outputSchema = metadata.getOutputSchema();
        Set<String> outputKeySet = outputSchema.getItems().keySet();
        Set<String> nameMappingOutputKeySet = nameMapping.getOutputMapping().keySet();

        if (!inputKeySet.containsAll(nameMappingInputKeySet)) {
            Set<String> copyNameMapping = new HashSet<>(nameMappingInputKeySet);
            copyNameMapping.removeAll(inputKeySet);
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Not all mapped input fields exist in model metadata, input features " + copyNameMapping + " do not exist")
                    .build();
        }

        if (!outputKeySet.containsAll(nameMappingOutputKeySet)) {
            Set<String> copyNameMapping = new HashSet<>(nameMappingOutputKeySet);
            copyNameMapping.removeAll(outputKeySet);
            return Response.serverError()
                    .status(Response.Status.BAD_REQUEST)
                    .entity("Not all mapped output fields exist in model metadata, output fields " + copyNameMapping + " do not exist")
                    .build();
        }

        inputSchema.setNameMapping(nameMapping.getInputMapping());
        outputSchema.setNameMapping(nameMapping.getOutputMapping());
        System.out.println("Saving metadata");
        dataSource.get().saveMetadata(metadata, metadata.getModelId());

        return Response.ok().entity("Feature and output name mapping successfully applied.").build();
    }

}
