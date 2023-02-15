package org.kie.trustyai.service.data.exceptions;

public class DataframeCreateException extends RuntimeException {
    public DataframeCreateException(String errorMessage) {
        super(errorMessage);
    }
}
