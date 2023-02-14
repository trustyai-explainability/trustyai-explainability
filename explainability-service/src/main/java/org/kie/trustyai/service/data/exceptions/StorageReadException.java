package org.kie.trustyai.service.data.exceptions;

public class StorageReadException extends Exception {
    public StorageReadException(String errorMessage) {
        super(errorMessage);
    }
}
