package org.kie.trustyai.service.prometheus;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.endpoints.metrics.MetricsDirectory;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.RequestReconciler;

import io.quarkus.scheduler.Scheduled;
import io.vertx.core.impl.ConcurrentHashSet;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class PrometheusScheduler {

    private static final Logger LOG = Logger.getLogger(PrometheusScheduler.class);
    private final ConcurrentHashMap<String, ConcurrentHashMap<UUID, BaseMetricRequest>> requests = new ConcurrentHashMap<>();
    private final ConcurrentHashSet<String> hasLoggedSkippedRequestMessage = new ConcurrentHashSet<>();
    private final MetricsDirectory metricsDirectory = new MetricsDirectory();
    @Inject
    protected PrometheusPublisher publisher;
    @Inject
    Instance<DataSource> dataSource;
    @Inject
    ServiceConfig serviceConfig;

    public MetricsDirectory getMetricsDirectory() {
        return metricsDirectory;
    }

    public Map<UUID, BaseMetricRequest> getRequests(String metricName) {
        return Collections.unmodifiableMap(this.requests.getOrDefault(metricName, new ConcurrentHashMap<>()));
    }

    public Map<UUID, BaseMetricRequest> getAllRequestsFlat() {
        ConcurrentHashMap<UUID, BaseMetricRequest> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, ConcurrentHashMap<UUID, BaseMetricRequest>> metricDict : requests.entrySet()) {
            result.putAll(metricDict.getValue());
        }
        return result;
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<UUID, BaseMetricRequest>> getAllRequests() {
        return requests;
    }

    @Scheduled(every = "{service.metrics-schedule}")
    void calculate() {
        calculateManual(false);
    }

    /**
     * Calculate scheduled metrics
     *
     * @param throwErrors if true, errors will be thrown. If false, they will just be logged and calculation will continue.
     */
    public void calculateManual(boolean throwErrors) {
        try {
            // global service statistic
            DataSource ds = dataSource.get();
            List<String> verifiedModels = ds.getVerifiedModels();
            publisher.gauge("", "MODEL_COUNT_TOTAL", UUID.nameUUIDFromBytes("model_count".getBytes(StandardCharsets.UTF_8)), verifiedModels.size());

            Set<String> requestedModels = getModelIds();
            for (final String modelId : verifiedModels) {
                // global model statistics
                publisher.gauge(modelId, "MODEL_OBSERVATIONS_TOTAL", UUID.nameUUIDFromBytes(modelId.getBytes(StandardCharsets.UTF_8)), ds.getNumObservations(modelId));

                boolean hasRecordedInferences = dataSource.get().hasRecordedInferences(modelId);

                if (!hasRecordedInferences) {
                    if (!hasLoggedSkippedRequestMessage.contains(modelId)) {
                        LOG.info("Skipping metric calculation for model=" + modelId + ", as no inference data has yet been recorded. Once inference data arrives, metric calculation will resume.");
                        hasLoggedSkippedRequestMessage.add(modelId);
                    }
                    continue;
                }

                if (hasRequests() && requestedModels.contains(modelId)) {
                    final Predicate<Map.Entry<UUID, BaseMetricRequest>> filterByModelId = request -> request.getValue().getModelId().equals(modelId);
                    List<Map.Entry<UUID, BaseMetricRequest>> requestsSet = getAllRequestsFlat().entrySet().stream().filter(filterByModelId).collect(Collectors.toList());

                    // Determine maximum batch requested. All other batches as sub-batches of this one.
                    final int maxBatchSize = requestsSet.stream()
                            .mapToInt(entry -> entry.getValue().getBatchSize()).max()
                            .orElse(serviceConfig.batchSize().getAsInt());
                    final Dataframe df = ds.getOrganicDataframe(modelId, maxBatchSize);

                    requestsSet.forEach(entry -> {
                        // entry value: BaseMetricRequest
                        final Dataframe batch = df.tail(Math.min(df.getRowDimension(), entry.getValue().getBatchSize()));
                        String metricName = entry.getValue().getMetricName();
                        final MetricValueCarrier value = metricsDirectory.getCalculator(metricName).apply(batch, entry.getValue());
                        if (value.isSingle()) {
                            publisher.gauge(entry.getValue(), modelId, entry.getKey(), value.getValue());
                        } else {
                            publisher.gauge(entry.getValue(), modelId, entry.getKey(), value.getNamedValues());
                        }
                    });
                }
            }
        } catch (DataframeCreateException e) {
            if (throwErrors) {
                throw e;
            } else {
                LOG.error(e.getMessage());
            }
        }
    }

    private PrometheusPublisher getPublisher() {
        return publisher;
    }

    public void register(String metricName, UUID id, BaseMetricRequest request) {
        if (!requests.containsKey(metricName)) {
            requests.put(metricName, new ConcurrentHashMap<>());
        }
        RequestReconciler.reconcile(request, dataSource);
        requests.get(metricName).put(id, request);
    }

    public void delete(String metricName, UUID id) {
        this.requests.getOrDefault(metricName, new ConcurrentHashMap<>()).remove(id);
        this.getPublisher().removeGauge(metricName, id);
    }

    public boolean hasRequests() {
        return !requests.isEmpty();
    }

    /**
     * Get unique model ids with registered Prometheus metrics
     *
     * @return Unique models ids
     */
    public Set<String> getModelIds() {
        return Stream.of(getAllRequestsFlat().values())
                .flatMap(Collection::stream)
                .map(BaseMetricRequest::getModelId)
                .collect(Collectors.toSet());
    }
}
