package org.kie.trustyai.service.data.exceptions;

public class PayloadWriteException extends RuntimeException {
    public PayloadWriteException(String errorMessage) {
        super(errorMessage);
    }
}
