package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;

import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

public interface Storage {

    ByteBuffer getInputData() throws StorageReadException;

    ByteBuffer getOutputData() throws StorageReadException;

    void saveInputData(ByteBuffer inputData) throws StorageWriteException, StorageReadException;

    void saveOutputData(ByteBuffer outputData) throws StorageWriteException, StorageReadException;
}
