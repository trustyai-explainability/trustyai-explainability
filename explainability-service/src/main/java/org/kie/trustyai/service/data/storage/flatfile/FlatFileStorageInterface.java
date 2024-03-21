package org.kie.trustyai.service.data.storage.flatfile;

import java.nio.ByteBuffer;

import org.kie.trustyai.service.data.cache.DataCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.storage.StorageInterface;

import io.quarkus.cache.CacheResult;

public interface FlatFileStorageInterface extends StorageInterface {
    @CacheResult(cacheName = "dataframe", keyGenerator = DataCacheKeyGen.class)
    ByteBuffer readData(String modelId) throws StorageReadException;

    ByteBuffer readData(String modelId, int batchSize) throws StorageReadException;

    ByteBuffer readData(String modelId, int startPos, int endPos) throws StorageReadException;

    boolean dataExists(String modelId) throws StorageReadException;

    void save(ByteBuffer data, String location) throws StorageWriteException;

    void append(ByteBuffer data, String location) throws StorageWriteException;

    void appendData(ByteBuffer data, String modelId) throws StorageWriteException;

    ByteBuffer read(String location) throws StorageReadException;

    ByteBuffer read(String location, int startPos, int endPos) throws StorageReadException;

    void saveData(ByteBuffer data, String modelId) throws StorageWriteException;

    boolean fileExists(String location) throws StorageReadException;
}
