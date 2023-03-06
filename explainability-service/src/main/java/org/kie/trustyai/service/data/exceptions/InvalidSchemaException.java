package org.kie.trustyai.service.data.exceptions;

public class InvalidSchemaException extends RuntimeException {
    public InvalidSchemaException(String errorMessage) {
        super(errorMessage);
    }
}
