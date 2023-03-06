package org.kie.trustyai.service.data.exceptions;

public class StorageWriteException extends RuntimeException {
    public StorageWriteException(String errorMessage) {
        super(errorMessage);
    }
}
