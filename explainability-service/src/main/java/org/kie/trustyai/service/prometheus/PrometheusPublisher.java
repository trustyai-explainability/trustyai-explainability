package org.kie.trustyai.service.prometheus;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
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

    private List<Tag> generateTags(String modelName, UUID id, Optional<BaseMetricRequest> request) {
        List<Tag> tags;
        if (request.isPresent()) {
            Map<String, String> tagMap = request.get().retrieveDefaultTags();
            tagMap.putAll(request.get().retrieveTags());
            tags = tagMap.entrySet().stream()
                    .map(e -> Tag.of(e.getKey(), e.getValue()))
                    .collect(Collectors.toList());
        } else {
            tags = new ArrayList<>();
            if (!modelName.isEmpty()) {
                // populate the model name if not coming via a request object
                tags.add(Tag.of("model", modelName));
            }
        }
        tags.add(Tag.of("request", id.toString()));
        return tags;
    }

    public void gauge(@ValidReconciledMetricRequest BaseMetricRequest request, String modelName, UUID id, double value) {
        values.put(id, new AtomicDouble(value));
        Iterable<Tag> tags = Tags.of(generateTags(modelName, id, Optional.of(request)));
        createOrUpdateGauge(METRIC_PREFIX + request.getMetricName().toLowerCase(), tags, id);
        LOG.debug(String.format("Scheduled request for %s id=%s, value=%f", request.getMetricName(), id, value));
    }

    public void gauge(@ValidReconciledMetricRequest BaseMetricRequest request, String modelName, UUID id, Map<String, Double> namedValues) {
        AtomicInteger idx = new AtomicInteger();
        for (Map.Entry<String, Double> entry : namedValues.entrySet()) {
            UUID newID = UUID.nameUUIDFromBytes((id.toString() + idx.getAndIncrement()).getBytes(StandardCharsets.UTF_8));
            values.put(newID, new AtomicDouble(entry.getValue()));
            List<Tag> tags = generateTags(modelName, newID, Optional.of(request));
            tags.add(Tag.of("subcategory", entry.getKey()));
            createOrUpdateGauge(METRIC_PREFIX + request.getMetricName().toLowerCase(), tags, newID);
        }
        LOG.debug(String.format("Scheduled request for %s id=%s, value=%s", request.getMetricName(), id, namedValues));
    }

    public void gauge(String modelName, String metricName, UUID id, double value) {
        values.put(id, new AtomicDouble(value));
        Iterable<Tag> tags = Tags.of(generateTags(modelName, id, Optional.empty()));
        createOrUpdateGauge(METRIC_PREFIX + metricName.toLowerCase(), tags, id);
        LOG.debug(String.format("Scheduled request for %s id=%s, value=%f", metricName, id, value));
    }

}
