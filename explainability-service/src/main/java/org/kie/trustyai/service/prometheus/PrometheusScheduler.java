package org.kie.trustyai.service.prometheus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.endpoints.metrics.MetricsCalculator;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceRequest;

import io.quarkus.scheduler.Scheduled;

@Singleton
public class PrometheusScheduler {

    private static final Logger LOG = Logger.getLogger(PrometheusScheduler.class);
    private final Map<UUID, GroupStatisticalParityDifferenceRequest> spdRequests = new HashMap<>();
    private final Map<UUID, GroupStatisticalParityDifferenceRequest> dirRequests = new HashMap<>();
    @Inject
    DataSource dataSource;
    @Inject
    PrometheusPublisher publisher;
    @Inject
    ServiceConfig serviceConfig;
    @Inject
    MetricsCalculator calculator;

    public Map<UUID, GroupStatisticalParityDifferenceRequest> getDirRequests() {
        return dirRequests;
    }

    public Map<UUID, GroupStatisticalParityDifferenceRequest> getSpdRequests() {
        return spdRequests;
    }

    @Scheduled(every = "{SERVICE_METRICS_SCHEDULE}")
    void calculate() {

        if (hasRequests()) {
            try {
                final Dataframe df = dataSource.getDataframe();

                // SPD requests
                if (!spdRequests.isEmpty()) {
                    spdRequests.forEach((uuid, request) -> {

                        final double spd = calculator.calculateSPD(df, request);

                        publisher.gaugeSPD(request, serviceConfig.modelName(), uuid, spd);
                    });
                }

                // DIR requests
                if (!dirRequests.isEmpty()) {
                    dirRequests.forEach((uuid, request) -> {

                        final double dir = calculator.calculateDIR(df, request);
                        publisher.gaugeDIR(request, serviceConfig.modelName(), uuid, dir);
                    });
                }
            } catch (DataframeCreateException e) {
                LOG.error(e.getMessage());
            }
        }
    }

    public void registerSPD(UUID id, GroupStatisticalParityDifferenceRequest request) {
        spdRequests.put(id, request);
    }

    public void registerDIR(UUID id, GroupStatisticalParityDifferenceRequest request) {
        dirRequests.put(id, request);
    }

    public boolean hasRequests() {
        return !(spdRequests.isEmpty() && dirRequests.isEmpty());
    }
}
