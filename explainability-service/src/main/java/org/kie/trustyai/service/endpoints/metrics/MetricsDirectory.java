package org.kie.trustyai.service.endpoints.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

public class MetricsDirectory {
    private final Map<String, BiFunction<Dataframe, @ValidReconciledMetricRequest BaseMetricRequest, MetricValueCarrier>> calculatorDirectory;

    public MetricsDirectory() {
        /* No attributes to initialize at initialization */
        calculatorDirectory = new HashMap<>();
    }

    public void register(String name, BiFunction<Dataframe, @ValidReconciledMetricRequest BaseMetricRequest, MetricValueCarrier> function) {
        if (!calculatorDirectory.containsKey(name)) {
            calculatorDirectory.put(name, function);
        }
    }

    public BiFunction<Dataframe, @ValidReconciledMetricRequest BaseMetricRequest, MetricValueCarrier> getCalculator(String name) {
        return calculatorDirectory.get(name);
    }
}
