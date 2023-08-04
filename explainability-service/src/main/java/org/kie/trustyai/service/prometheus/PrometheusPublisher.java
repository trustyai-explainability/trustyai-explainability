package org.kie.trustyai.service.prometheus;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    public void removeGauge(String name, UUID id){
        List<Gauge> gaugesToDelete = registry.get("trustyai_"+name.toLowerCase())
                .gauges().stream().filter(
                        gauge -> gauge.getId().getTags().stream()
                                .anyMatch(t -> t.getKey().equals("request") && t.getValue().equals(id.toString()))
                )
                .collect(Collectors.toList());
        for (Gauge g: gaugesToDelete){
            registry.remove(g);
        }
    }

    private Iterable<Tag> generateTags(String modelName, UUID id, BaseMetricRequest request) {
        List<Tag> tags = request.retrieveTags().entrySet().stream()
                .map(e -> Tag.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
        tags.add(Tag.of("request", id.toString()));
        tags.add(Tag.of("model", modelName));
        return Tags.of(tags);
    }

    public void gauge(@ValidReconciledMetricRequest BaseMetricRequest request, String modelName, UUID id, double value) {

        values.put(id, new AtomicDouble(value));

        final Iterable<Tag> tags = generateTags(modelName, id, request);

        createOrUpdateGauge("trustyai_" + request.getMetricName().toLowerCase(), tags, id);

        LOG.info(String.format("Scheduled request for %s id=%s, value=%f", request.getMetricName(), id, value));
    }
}
