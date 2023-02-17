package org.kie.trustyai.service.data.exceptions;

public class MetricCalculationException extends RuntimeException {
    public MetricCalculationException(String errorMessage) {
        super(errorMessage);
    }
}
