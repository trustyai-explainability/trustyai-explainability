package org.kie.trustyai.service.prometheus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.metrics.ReconciledBaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;

import io.quarkus.scheduler.Scheduled;

@Singleton
public class PrometheusScheduler {

    private static final Logger LOG = Logger.getLogger(PrometheusScheduler.class);
    private final ConcurrentHashMap<String, ConcurrentHashMap<UUID, ReconciledBaseMetricRequest>> requests = new ConcurrentHashMap<>();
    @Inject
    Instance<DataSource> dataSource;
    @Inject
    PrometheusPublisher publisher;

    @Inject
    ServiceConfig serviceConfig;

    public Map<UUID, ReconciledBaseMetricRequest> getRequests(String metricName) {
        return this.requests.get(metricName);
    }


    public Map<UUID, ReconciledBaseMetricRequest> getAllRequests() {
        // extend this with other metrics when more are added=
        ConcurrentHashMap<UUID, ReconciledBaseMetricRequest> result = new ConcurrentHashMap<>();
        for (Map.Entry<String, ConcurrentHashMap<UUID, ReconciledBaseMetricRequest>> metricDict : requests.entrySet()){
            result.putAll(metricDict.getValue());
        }
        return result;
    }

    @Scheduled(every = "{service.metrics-schedule}")
    void calculate() {

        if (hasRequests()) {

            try {
                for (final String modelId : getModelIds()) {

                    final Predicate<Map.Entry<UUID, ReconciledBaseMetricRequest>> filterByModelId = request -> request.getValue().getModelId().equals(modelId);

                    Set<Map.Entry<UUID, ReconciledBaseMetricRequest>> requestsSet = getAllRequests().entrySet();

                    // Determine maximum batch requested. All other batches as sub-batches of this one.
                    final int maxBatchSize = requestsSet.stream()
                            .mapToInt(entry -> entry.getValue().getBatchSize()).max()
                            .orElse(serviceConfig.batchSize().getAsInt());

                    final Dataframe df = dataSource.get().getDataframe(modelId, maxBatchSize);

                    requestsSet.forEach(entry -> {
                        final Dataframe batch = df.tail(Math.min(df.getRowDimension(), entry.getValue().getBatchSize()));
                        final double spd = entry.getValue().calculate(batch, entry.getValue());
                        publisher.gauge(entry.getValue(), modelId, entry.getKey(), spd);
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

    public void register(String metricName, UUID id, ReconciledGroupMetricRequest request) {
        if (!requests.containsKey(metricName)){
            requests.put(metricName, new ConcurrentHashMap<>());
        }
        requests.get(metricName).put(id, request);
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
        return Stream.of(getAllRequests().values())
                .flatMap(Collection::stream)
                .map(ReconciledBaseMetricRequest::getModelId)
                .collect(Collectors.toSet());
    }
}
