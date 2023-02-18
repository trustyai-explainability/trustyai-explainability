package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;

import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

public interface StorageInterface {
    ByteBuffer getInputData() throws StorageReadException;

    ByteBuffer getOutputData() throws StorageReadException;

    void saveInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException;

    void saveOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException;

    void appendInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException;

    void appendOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException;

    boolean inputExists() throws StorageReadException;

    boolean outputExists() throws StorageReadException;

}
