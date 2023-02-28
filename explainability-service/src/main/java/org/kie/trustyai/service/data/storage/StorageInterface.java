package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;

import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

public interface StorageInterface {
    ByteBuffer getData(String modelId) throws StorageReadException;

    boolean dataExists(String modelId) throws StorageReadException;

    void save(ByteBuffer data, String location) throws StorageWriteException;

    void append(ByteBuffer data, String location) throws StorageWriteException;

    void appendData(ByteBuffer data, String modelId) throws StorageWriteException;

    ByteBuffer read(String location) throws StorageReadException;

    void saveData(ByteBuffer data, String modelId) throws StorageWriteException;

    boolean fileExists(String location) throws StorageReadException;

}
