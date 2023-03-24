package org.kie.trustyai.service.prometheus;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.endpoints.metrics.MetricsCalculator;
import org.kie.trustyai.service.payloads.BaseMetricRequest;
import org.kie.trustyai.service.payloads.service.SchemaItem;

import io.quarkus.scheduler.Scheduled;

@Singleton
public class PrometheusScheduler {

    private static final Logger LOG = Logger.getLogger(PrometheusScheduler.class);
    private final Map<UUID, BaseMetricRequest> spdRequests = new HashMap<>();
    private final Map<UUID, BaseMetricRequest> dirRequests = new HashMap<>();
    @Inject
    Instance<DataSource> dataSource;
    @Inject
    PrometheusPublisher publisher;
    @Inject
    MetricsCalculator calculator;

    public Map<UUID, BaseMetricRequest> getDirRequests() {
        return dirRequests;
    }

    public Map<UUID, BaseMetricRequest> getSpdRequests() {
        return spdRequests;
    }

    @Scheduled(every = "{service.metrics-schedule}")
    void calculate() {

        if (hasRequests()) {

            try {
                for (final String modelId : getModelIds()) {

                    final Predicate<Map.Entry<UUID, BaseMetricRequest>> filterByModelId = request -> request.getValue().getModelId().equals(modelId);

                    final Dataframe df = dataSource.get().getDataframe(modelId);
                    final List<Map.Entry<UUID, BaseMetricRequest>> modelSpdRequest =
                            spdRequests.entrySet().stream().filter(filterByModelId).collect(Collectors.toList());

                    // SPD requests
                    modelSpdRequest.forEach(entry -> {
                        final double spd = calculator.calculateSPD(df, entry.getValue());
                        publisher.gaugeSPD(entry.getValue(), modelId, entry.getKey(), spd);
                    });

                    final List<Map.Entry<UUID, BaseMetricRequest>> modelDirRequest =
                            dirRequests.entrySet().stream().filter(filterByModelId).collect(Collectors.toList());

                    // DIR requests

                    modelDirRequest.forEach(entry -> {
                        final double dir = calculator.calculateDIR(df, entry.getValue());
                        publisher.gaugeDIR(entry.getValue(), modelId, entry.getKey(), dir);
                    });

                }
            } catch (DataframeCreateException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    public PrometheusPublisher getPublisher() {
        return publisher;
    }

    public void registerSPD(UUID id, BaseMetricRequest request) {
        spdRequests.put(id, request);
    }

    public void registerDIR(UUID id, BaseMetricRequest request) {
        dirRequests.put(id, request);
    }

    public boolean hasRequests() {
        return !(spdRequests.isEmpty() && dirRequests.isEmpty());
    }

    /**
     * Get unique model ids with registered Prometheus metrics
     * 
     * @return Unique models ids
     */
    public Set<String> getModelIds() {
        return Stream.of(spdRequests.values(), dirRequests.values())
                .flatMap(Collection::stream)
                .map(BaseMetricRequest::getModelId)
                .collect(Collectors.toSet());
    }

    public void validateRequest(BaseMetricRequest request) throws InvalidSchemaException {
        final String modelId = request.getModelId();
        if (!dataSource.get().hasMetadata(modelId)) {
            throw new InvalidSchemaException("No metadata found for model=" + modelId);
        } else {
            final Metadata metadata = dataSource.get().getMetadata(modelId);
            final String outcomeName = request.getOutcomeName();
            // Outcome name is not present
            if (metadata.getOutputSchema().getItems().stream().noneMatch(item -> Objects.equals(item.getName(), outcomeName))) {
                throw new InvalidSchemaException("No outcome found with name=" + outcomeName);
            }
            final String protectedAttribute = request.getProtectedAttribute();
            if (metadata.getInputSchema().getItems().stream().noneMatch(item -> Objects.equals(item.getName(), protectedAttribute))) {
                throw new InvalidSchemaException("No protected attribute found with name=" + protectedAttribute);
            }
            // Outcome name guaranteed to exist
            final SchemaItem outcomeSchema = metadata.getOutputSchema().getItems().stream().filter(item -> item.getName().equals(outcomeName)).findFirst().get();
            if (!outcomeSchema.getType().equals(request.getFavorableOutcome().getType())) {
                throw new InvalidSchemaException("Invalid type for outcome. Got '" + request.getFavorableOutcome().getType().toString() + "', expected '" + outcomeSchema.getType().toString() + "'");
            }
            // Protected attribute guaranteed to exist
            final SchemaItem protectedAttrSchema = metadata.getInputSchema().getItems().stream().filter(item -> item.getName().equals(protectedAttribute)).findFirst().get();
            if (!protectedAttrSchema.getType().equals(request.getPrivilegedAttribute().getType())) {
                throw new InvalidSchemaException(
                        "Invalid type for privileged attribute. Got '" + request.getPrivilegedAttribute().getType().toString() + "', expected '" + protectedAttrSchema.getType().toString() + "'");
            }
            if (!protectedAttrSchema.getType().equals(request.getUnprivilegedAttribute().getType())) {
                throw new InvalidSchemaException(
                        "Invalid type for unprivileged attribute. Got '" + request.getUnprivilegedAttribute().getType().toString() + "', expected '" + protectedAttrSchema.getType().toString() + "'");
            }
        }
    }
}
