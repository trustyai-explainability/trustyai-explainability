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
import org.kie.trustyai.service.endpoints.metrics.MetricsCalculator;
import org.kie.trustyai.service.payloads.BaseMetricRequest;

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
}
