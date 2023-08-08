package org.kie.trustyai.service.prometheus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

import com.google.common.util.concurrent.AtomicDouble;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.Search;

@Singleton
public class PrometheusPublisher {
    private static final Logger LOG = Logger.getLogger(PrometheusPublisher.class);
    private final MeterRegistry registry;
    private final Map<UUID, AtomicDouble> values;
    private static final String METRIC_PREFIX = "trustyai_";

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

    public void removeGauge(String name, UUID id) {
        Search s = this.registry.find(METRIC_PREFIX + name.toLowerCase());
        Collection<Gauge> gaugesToDelete = s.tags(List.of(Tag.of("request", id.toString()))).gauges();
        for (Gauge g : gaugesToDelete) {
            registry.remove(g);
        }
    }

    private Iterable<Tag> generateTags(String modelName, UUID id, BaseMetricRequest request) {
        List<Tag> tags;
        if (request != null) {
            tags = request.retrieveTags().entrySet().stream()
                    .map(e -> Tag.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } else {
            tags = new ArrayList<>();
        }
        tags.add(Tag.of("request", id.toString()));
        if (!modelName.isEmpty()) {
            tags.add(Tag.of("model", modelName));
        }
        return Tags.of(tags);
    }

    public void gauge(@ValidReconciledMetricRequest BaseMetricRequest request, String modelName, UUID id, double value) {
        values.put(id, new AtomicDouble(value));
        final Iterable<Tag> tags = generateTags(modelName, id, request);
        createOrUpdateGauge(METRIC_PREFIX + request.getMetricName().toLowerCase(), tags, id);
        LOG.info(String.format("Scheduled request for %s id=%s, value=%f", request.getMetricName(), id, value));
    }

    public void gauge(String modelName, String metricName, UUID id, double value) {
        values.put(id, new AtomicDouble(value));
        final Iterable<Tag> tags = generateTags(modelName, id, null);
        createOrUpdateGauge(METRIC_PREFIX + metricName.toLowerCase(), tags, id);
        LOG.info(String.format("Scheduled request for %s id=%s, value=%f", metricName, id, value));
    }
}
