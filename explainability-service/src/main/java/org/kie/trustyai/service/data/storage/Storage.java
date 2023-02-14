package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;

import org.kie.trustyai.service.data.exceptions.StorageReadException;

public interface Storage {

    ByteBuffer getInputData() throws StorageReadException;

    ByteBuffer getOutputData() throws StorageReadException;

    void saveInputData(ByteBuffer inputData) throws StorageReadException;

    void saveOutputData(ByteBuffer outputData) throws StorageReadException;
}
