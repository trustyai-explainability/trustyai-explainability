package org.kie.trustyai.service.data.exceptions;

public class StorageWriteException extends Exception {
    public StorageWriteException(String errorMessage) {
        super(errorMessage);
    }
}
