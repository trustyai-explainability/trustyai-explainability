package org.kie.trustyai.service.data.exceptions;

public class StorageReadException extends RuntimeException {
    public StorageReadException(String errorMessage) {
        super(errorMessage);
    }
}
