package org.kie.trustyai.service.prometheus;

import java.util.Map;

public class MetricValueCarrier {
    private final Double value;
    private final Map<String, Double> namedValues;

    public MetricValueCarrier(double value) {
        this.value = value;
        this.namedValues = null;
    }

    public MetricValueCarrier(Map<String, Double> namedValues) {
        this.value = null;
        this.namedValues = namedValues;
    }

    public double getValue() {
        if (value != null) {
            return value;
        } else {
            throw new UnsupportedOperationException("Metric value is not singular and therefore must be accessed via .getNamedValues()");
        }
    }

    public Map<String, Double> getNamedValues() {
        if (namedValues != null) {
            return namedValues;
        } else {
            throw new UnsupportedOperationException("Metric value is singular and therefore must be accessed via .getValue()");
        }
    }

    public boolean isSingle() {
        return value != null;
    }
}
