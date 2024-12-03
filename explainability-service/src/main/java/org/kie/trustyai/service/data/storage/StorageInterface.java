package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.kie.trustyai.service.data.cache.DataCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.payloads.service.InferenceId;

import io.quarkus.cache.CacheResult;

public interface StorageInterface<DATAFRAME_TYPE, AUX_DATA_TYPE> {
    DataFormat getDataFormat();

    long getLastModified(String modelId);

    // dataframes ======================================================================================================
    @CacheResult(cacheName = "dataframe", keyGenerator = DataCacheKeyGen.class)
    DATAFRAME_TYPE readDataframe(String modelId) throws StorageReadException;

    DATAFRAME_TYPE readDataframe(String modelId, int batchSize) throws StorageReadException;

    DATAFRAME_TYPE readDataframe(String modelId, int startPos, int endPos) throws StorageReadException;

    void saveDataframe(DATAFRAME_TYPE dataframe, String modelId) throws StorageWriteException;

    //    // filtered data reads
    // pull these into the interface when corresponding methods for flatfiles are written
    //    DATAFRAME_TYPE readNonSyntheticDataframe(String modelId) throws StorageReadException;
    //
    //    DATAFRAME_TYPE readNonSyntheticDataframe(String modelId, int batchSize) throws StorageReadException;

    // dataframe appenders =============================================================================================
    void append(DATAFRAME_TYPE dataframe, String location) throws StorageWriteException;

    // metadata or internal data =======================================================================================
    AUX_DATA_TYPE readMetaOrInternalData(String modelId) throws StorageReadException;

    void saveMetaOrInternalData(AUX_DATA_TYPE auxData, String modelId) throws StorageWriteException;

    // info queries ====================================================================================================
    boolean dataExists(String modelId) throws StorageReadException;

    /**
     * Read data and metadata with the specified tags and batch size.
     *
     * @param modelId The model ID
     * @param batchSize The batch size
     * @param tags The tags
     * @return A pair of {@link ByteBuffer} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */
    Pair<DATAFRAME_TYPE, AUX_DATA_TYPE> readDataframeAndMetadataWithTags(String modelId, int batchSize, Set<String> tags) throws StorageReadException;

    /**
     * Read data and metadata with the specified ids.
     *
     * @param modelId The model ID
     * @param ids The ids
     * @return A pair of {@link ByteBuffer} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */
    Pair<DATAFRAME_TYPE, AUX_DATA_TYPE> readDataframeAndMetadataWithIds(String modelId, Set<String> ids) throws StorageReadException;

    /**
     * Read data and metadata without the specified ids.
     *
     * @param modelId The model ID
     * @param ids The ids
     * @return A pair of {@link ByteBuffer} containing the data and metadata without the ids
     * @throws StorageReadException If an error occurs while reading the data
     */
    Pair<DATAFRAME_TYPE, AUX_DATA_TYPE> readDataframeAndMetadataWithoutIds(String modelId, Set<String> ids) throws StorageReadException;

    /**
     * Read data and metadata with the specified tags and batch size.
     * Since no batch size is specified, the default batch size is used.
     *
     * @param modelId The model ID
     * @param tags The tags
     * @return A pair of {@link ByteBuffer} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */

    Pair<DATAFRAME_TYPE, AUX_DATA_TYPE> readDataframeAndMetadataWithTags(String modelId, Set<String> tags) throws StorageReadException;

    /**
     * Read data and metadata without the specified tags and batch size.
     *
     * @param modelId The model ID
     * @param batchSize The batch size
     * @param tags The tags that the returned rows should not contain
     * @return A pair of {@link ByteBuffer} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */
    Pair<DATAFRAME_TYPE, AUX_DATA_TYPE> readDataframeAndMetadataWithoutTags(String modelId, int batchSize, Set<String> tags) throws StorageReadException;

    /**
     * Read data and metadata without the specified tags and batch size.
     * Since no batch size is specified, the default batch size is used.
     *
     * @param modelId The model ID
     * @param tags The tags that the returned rows should not contain
     * @return A pair of {@link ByteBuffer} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */

    Pair<DATAFRAME_TYPE, AUX_DATA_TYPE> readDataframeAndMetadataWithoutTags(String modelId, Set<String> tags) throws StorageReadException;

    /**
     * Read all inference ids without batch size.
     *
     * @param modelId The model ID
     * @return A list of {@link org.kie.trustyai.service.payloads.service.InferenceId} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */
    List<InferenceId> readAllInferenceIds(String modelId) throws StorageReadException;

    /**
     * Read all organic inference ids without batch size.
     *
     * @param modelId The model ID
     * @return A list of {@link org.kie.trustyai.service.payloads.service.InferenceId} containing the data and metadata
     * @throws StorageReadException If an error occurs while reading the data
     */
    List<InferenceId> readAllOrganicInferenceIds(String modelId) throws StorageReadException;

}
