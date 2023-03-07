package org.kie.trustyai.service.prometheus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.payloads.BaseMetricRequest;

import com.google.common.util.concurrent.AtomicDouble;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

@Singleton
public class PrometheusPublisher {
    private static final Logger LOG = Logger.getLogger(PrometheusPublisher.class);
    private final MeterRegistry registry;
    private final Map<UUID, AtomicDouble> values;

    @Inject
    ServiceConfig serviceConfig;

    public PrometheusPublisher(MeterRegistry registry) {
        this.registry = registry;
        this.values = new HashMap<>();
    }

    private void createOrUpdateGauge(String name, Iterable<Tag> tags, UUID id) {
        Gauge.builder(name, new AtomicDouble(), value -> values.get(id).doubleValue())
                .tags(tags).strongReference(true).register(registry);

    }

    private Iterable<Tag> generateTags(String modelName, UUID id, BaseMetricRequest request) {
        return Tags.of(
                Tag.of("model", modelName),
                Tag.of("outcome", request.getOutcomeName()),
                Tag.of("favorable_value", request.getFavorableOutcome().toString()),
                Tag.of("protected", request.getProtectedAttribute()),
                Tag.of("privileged", request.getPrivilegedAttribute().toString()),
                Tag.of("unprivileged", request.getUnprivilegedAttribute().toString()),
                Tag.of("batch_size", String.valueOf(serviceConfig.batchSize().orElse(-1))),
                Tag.of("request", id.toString()));
    }

    public void gaugeSPD(BaseMetricRequest request, String modelName, UUID id, double value) {

        values.put(id, new AtomicDouble(value));

        final Iterable<Tag> tags = generateTags(modelName, id, request);

        createOrUpdateGauge("trustyai_spd", tags, id);

        LOG.info("Scheduled request for SPD id=" + id + ", value=" + value);
    }

    public void gaugeDIR(BaseMetricRequest request, String modelName, UUID id, double value) {

        values.put(id, new AtomicDouble(value));

        final Iterable<Tag> tags = generateTags(modelName, id, request);

        createOrUpdateGauge("trustyai_dir", tags, id);

        LOG.info("Scheduled request for DIR id=" + id + ", value=" + value);
    }
}
