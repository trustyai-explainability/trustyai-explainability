package org.kie.trustyai.service.prometheus;

import java.util.Map;

public class MetricValueCarrier {
    private final Double value;
    private final Map<String, Double> namedValues;
    private final boolean single;

    public MetricValueCarrier(double value) {
        this.value = value;
        this.namedValues = null;
        single = true;
    }

    public MetricValueCarrier(Map<String, Double> namedValues) {
        this.value = null;
        this.namedValues = namedValues;
        single = false;
    }

    public double getValue() {
        if (single) {
            return value;
        } else {
            throw new UnsupportedOperationException("Metric value is not singular and therefore must be accessed via .getNamedValues()");
        }
    }

    public Map<String, Double> getNamedValues() {
        if (!single) {
            return namedValues;
        } else {
            throw new UnsupportedOperationException("Metric value is singular and therefore must be accessed via .getValue()");
        }
    }

    public boolean isSingle() {
        return single;
    }
}
