package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.kie.trustyai.service.data.cache.DataCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.quarkus.cache.CacheResult;

public interface StorageInterface {
    @CacheResult(cacheName = "dataframe", keyGenerator = DataCacheKeyGen.class)
    ByteBuffer readData(String modelId) throws StorageReadException;

    ByteBuffer readData(String modelId, int batchSize) throws StorageReadException;

    /**
     * Read data and metadata with the specified tags and batch size.
     * @param modelId The model ID
     * @param batchSize The batch size
     * @param tags The tags
     * @return A pair of {@link ByteBuffer} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */
    Pair<ByteBuffer, ByteBuffer> readDataWithTags(String modelId, int batchSize, Set<String> tags) throws StorageReadException;

    /**
     * Read data and metadata with the specified tags and batch size.
     * Since no batch size is specified, the default batch size is used.
     * @param modelId The model ID
     * @param tags The tags
     * @return A pair of {@link ByteBuffer} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */
    Pair<ByteBuffer, ByteBuffer> readDataWithTags(String modelId, Set<String> tags) throws StorageReadException;

    boolean dataExists(String modelId) throws StorageReadException;

    void save(ByteBuffer data, String location) throws StorageWriteException;

    void append(ByteBuffer data, String location) throws StorageWriteException;

    void appendData(ByteBuffer data, String modelId) throws StorageWriteException;

    ByteBuffer read(String location) throws StorageReadException;

    /**
     * Read {@link ByteBuffer} from the file system, for a given filename and batch size.
     *
     * @param location The filename to read
     * @param batchSize The batch size
     * @return A {@link ByteBuffer} containing the data
     * @throws StorageReadException If an error occurs while reading the data
     */
    ByteBuffer read(String location, int batchSize) throws StorageReadException;

    void saveData(ByteBuffer data, String modelId) throws StorageWriteException;

    boolean fileExists(String location) throws StorageReadException;

    long getLastModified(String modelId);

}
