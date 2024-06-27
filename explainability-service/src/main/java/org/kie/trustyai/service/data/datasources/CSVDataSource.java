package org.kie.trustyai.service.data.datasources;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.storage.flatfile.FlatFileStorage;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.Schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.lookup.LookupUnlessProperty;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@LookupUnlessProperty(name = "service.storage.format", stringValue = "DATABASE")
public class CSVDataSource extends DataSource {
    @Inject
    Instance<FlatFileStorage> storage;

    Optional<FlatFileStorage> storageOverride = Optional.empty();

    // STORAGE OPERATIONS ==============================================================================================
    /**
     * Set a temporary override to the original DataSource storage. This is useful for DB migration, to temporarily over-
     * ride the DB datasource with the to-be-migrated flatfile storage
     */
    public void setStorageOverride(FlatFileStorage storage) {
        this.storageOverride = Optional.of(storage);
    }

    public void clearStorageOverride() {
        this.storageOverride = Optional.empty();
    }

    /**
     * Get the storage corresponding to this datasource, using an overriden storage if one is set.
     */
    protected FlatFileStorage getStorage() {
        if (storageOverride.isPresent()) {
            LOG.debug("Using overridden storage");
            return storageOverride.get();
        } else {
            return storage.get();
        }
    }

    // DATAFRAME READS =================================================================================================
    /**
     * Using the default batch size, get a dataframe consisting of the last $defaultBatchSize rows of data from
     * the corresponding model.
     *
     * @param modelId the model id
     * @return a dataframe with the last $defaultBatchSize rows of data from the corresponding model.
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframe(final String modelId) throws DataframeCreateException {
        FlatFileStorage ffst = getStorage();
        final ByteBuffer dataByteBuffer;
        try {
            dataByteBuffer = ffst.readDataframe(modelId);
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final ByteBuffer internalDataByteBuffer;
        try {
            internalDataByteBuffer = ffst.readMetaOrInternalData(modelId + "-" + INTERNAL_DATA_FILENAME);
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        // Fetch metadata, if not yet read
        final StorageMetadata storageMetadata;
        try {
            storageMetadata = getMetadata(modelId);
        } catch (StorageReadException e) {
            throw new DataframeCreateException("Could not parse metadata: " + e.getMessage());
        }

        Dataframe df = parser.toDataframe(dataByteBuffer, internalDataByteBuffer, storageMetadata);
        df.setColumnAliases(storageMetadata.getJointNameAliases());
        df.setInputTensorName(storageMetadata.getInputTensorName());
        df.setOutputTensorName(storageMetadata.getOutputTensorName());
        return df;
    }

    /**
     * Get a dataframe consisting of the last $batchSize rows of data from the corresponding model.
     *
     * @param modelId the model id
     * @param batchSize the batchSize
     * @return a dataframe with the last $batchSize rows of data from the corresponding model.
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframe(final String modelId, int batchSize) throws DataframeCreateException {
        FlatFileStorage ffst = getStorage();

        final ByteBuffer byteBuffer;
        try {
            byteBuffer = ffst.readDataframe(modelId, batchSize);
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final ByteBuffer internalDataByteBuffer;
        try {
            internalDataByteBuffer = ffst.readMetaOrInternalData(modelId + "-" + INTERNAL_DATA_FILENAME, batchSize);
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        // Fetch metadata, if not yet read
        final StorageMetadata storageMetadata;
        try {
            storageMetadata = getMetadata(modelId);
        } catch (StorageReadException e) {
            throw new DataframeCreateException("Could not parse metadata: " + e.getMessage());
        }

        Dataframe df = parser.toDataframe(byteBuffer, internalDataByteBuffer, storageMetadata);
        df.setColumnAliases(storageMetadata.getJointNameAliases());
        df.setInputTensorName(storageMetadata.getInputTensorName());
        df.setOutputTensorName(storageMetadata.getOutputTensorName());
        return df;
    }

    /**
     * Get a dataframe consisting of the rows of data between $startPos (inclusive) and $endPos (exclusive).
     *
     * @param modelId the model id
     * @param startPos the beginning index to return in the dataframe slice, inclusive
     * @param endPos the ending index to return in the dataframe slice, exclusive
     *
     * @return a dataframe with rows of data between $startPos (inclusive) and $endPos (exclusive).
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframe(final String modelId, int startPos, int endPos) throws DataframeCreateException {
        if (endPos <= startPos) {
            throw new IllegalArgumentException("DataSource.getDataframe endPos must be greater than startPos. Got startPos=" + startPos + ", endPos=" + endPos);
        }

        FlatFileStorage ffst = getStorage();
        final ByteBuffer byteBuffer;
        try {
            byteBuffer = ffst.readDataframe(modelId, startPos, endPos);
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final ByteBuffer internalDataByteBuffer;
        try {
            internalDataByteBuffer = ffst.readMetaOrInternalData(modelId + "-" + INTERNAL_DATA_FILENAME, startPos, endPos);
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        // Fetch metadata, if not yet read
        final StorageMetadata storageMetadata;
        try {
            storageMetadata = getMetadata(modelId);
        } catch (StorageReadException e) {
            throw new DataframeCreateException("Could not parse metadata: " + e.getMessage());
        }

        Dataframe df = parser.toDataframe(byteBuffer, internalDataByteBuffer, storageMetadata);
        df.setColumnAliases(storageMetadata.getJointNameAliases());
        df.setInputTensorName(storageMetadata.getInputTensorName());
        df.setOutputTensorName(storageMetadata.getOutputTensorName());
        return df;

    }

    /**
     * Get a dataframe with the organic (non-synthetic) data and metadata for a given model
     *
     * @param modelId the model id
     * @param batchSize the batch size
     * @return a dataframe with the organic data and metadata for a given model
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getOrganicDataframe(final String modelId, int batchSize) throws DataframeCreateException {

        FlatFileStorage ffst = getStorage();
        final Pair<ByteBuffer, ByteBuffer> pair;
        try {
            pair = ffst.readDataframeAndMetadataWithTags(modelId, batchSize, Set.of(Dataframe.InternalTags.UNLABELED.get()));
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        // Fetch metadata, if not yet read
        final StorageMetadata metadata;
        try {
            metadata = getMetadata(modelId);
        } catch (StorageReadException e) {
            throw new DataframeCreateException("Could not parse metadata: " + e.getMessage());
        }

        Dataframe df;
        try {
            df = parser.toDataframe(pair.getLeft(), pair.getRight(), metadata);
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            throw new DataframeCreateException("Could not parse create dataframe: " + e.getMessage());
        }

        df.setColumnAliases(metadata.getJointNameAliases());
        df.setInputTensorName(metadata.getInputTensorName());
        df.setOutputTensorName(metadata.getOutputTensorName());
        return df;
    }

    /**
     * Get a dataframe with the organic (non-synthetic) data and metadata for a given model.
     * No batch size is given, so the default batch size is used.
     *
     * @param modelId the model id
     * @return a dataframe with the organic data and metadata for a given model
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getOrganicDataframe(final String modelId) throws DataframeCreateException {
        FlatFileStorage ffst = getStorage();
        final Pair<ByteBuffer, ByteBuffer> pair;
        try {
            pair = ffst.readDataframeAndMetadataWithTags(modelId, Set.of(Dataframe.InternalTags.UNLABELED.get()));
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        // Fetch metadata, if not yet read
        final StorageMetadata metadata;
        try {
            metadata = getMetadata(modelId);
        } catch (StorageReadException e) {
            throw new DataframeCreateException("Could not parse metadata: " + e.getMessage());
        }

        Dataframe df;
        try {
            df = parser.toDataframe(pair.getLeft(), pair.getRight(), metadata);
        } catch (IllegalArgumentException e) {
            LOG.error(e.getMessage());
            throw new DataframeCreateException("Could not parse create dataframe: " + e.getMessage());
        }

        df.setColumnAliases(metadata.getJointNameAliases());
        df.setInputTensorName(metadata.getInputTensorName());
        df.setOutputTensorName(metadata.getOutputTensorName());
        return df;
    }

    // DATAFRAME WRITES ================================================================================================
    protected void saveDataframeIntoStorage(final Dataframe dataframe, final String modelId, boolean overwrite) {
        FlatFileStorage ffst = getStorage();
        ByteBuffer[] byteBuffers = parser.toByteBuffers(dataframe, false);
        if (!ffst.dataExists(modelId) || overwrite) {
            ffst.saveMetaOrInternalData(byteBuffers[0], modelId);
            ffst.saveDataframe(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
        } else {
            ffst.appendMetaOrInternalData(byteBuffers[0], modelId);
            ffst.append(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
        }
    }

    // METADATA READS ==================================================================================================
    /**
     * Get metadata for this modelId, with optional loading of column enumerations
     *
     * @param modelId the model id
     * @param loadColumnValues if true, add column enumerations to the metadata. This adds an additional storage read,
     *        so use this only when necessary.
     * @throws StorageReadException if the metadata cannot be read
     */
    public StorageMetadata getMetadata(String modelId, boolean loadColumnValues) throws StorageReadException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        final ByteBuffer metadataBytes = getStorage().readMetaOrInternalData(modelId + "-" + METADATA_FILENAME);
        try {
            StorageMetadata res = mapper.readValue(new String(metadataBytes.array(), StandardCharsets.UTF_8), StorageMetadata.class);
            return res;
        } catch (JsonProcessingException e) {
            LOG.error("Could not parse metadata: " + e.getMessage());
            throw new StorageReadException(e.getMessage());
        }
    }

    /**
     * Check whether metadata exists for this modelId
     *
     * @param modelId the modelId to check for
     * @return true if metadata exists, false otherwise
     */
    public boolean hasMetadata(String modelId) {
        return getStorage().fileExists(modelId + "-" + METADATA_FILENAME);
    }

    // METADATA WRITES =================================================================================================
    /**
     * Save metadata for this modelId
     *
     * @param storageMetadata the metadata to save
     * @param modelId the modelId to save this metadata under.
     * @throws StorageWriteException if the metadata cannot be saved.
     */
    public void saveMetadata(StorageMetadata storageMetadata, String modelId) throws StorageWriteException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        final ByteBuffer byteBuffer;
        try {
            byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(storageMetadata).getBytes());
        } catch (JsonProcessingException e) {
            throw new StorageWriteException("Could not save metadata: " + e.getMessage());
        }

        getStorage().saveDataframe(byteBuffer, modelId + "-" + METADATA_FILENAME);
    }

    // DATAFRAME QUERIES ===============================================================================================
    /**
     * Get the number of observations for the corresponding model.
     *
     * @param modelId the modelId to get the observation count for.
     * @return the number of observations
     */
    public long getNumObservations(String modelId) {
        return getMetadata(modelId).getObservations();
    }

    /**
     * Check to see if a particular model has recorded inferences
     *
     * @param modelId the modelId to check
     * @return true if the model has received inference data
     */
    public boolean hasRecordedInferences(String modelId) {
        return getMetadata(modelId).isRecordedInferences();
    }

    // TAG OPERATIONS ==================================================================================================
    /**
     * Tag rows of a dataframe according to the tag mapping.
     *
     * @param dataTagging the tagMapping to apply. This contains both the modelId and the corresponding tag labels.
     */
    public void tagDataframeRows(DataTagging dataTagging) {
        Dataframe df = getDataframe(dataTagging.getModelId());
        df.tagDataPoints(dataTagging.getDataTagging());
        saveDataframe(df, dataTagging.getModelId(), true);
    }

    @Override
    public List<String> getTags(String modelId) {
        int numObs = (int) getNumObservations(modelId);
        return getDataframe(modelId, numObs).getTags();
    }

    // NAME MAPPING OPERATIONS =========================================================================================
    /**
     * Apply a name mapping to a dataframe
     *
     * @param nameMapping the nameMapping to apply. This contains both the modelId and the name mappings.
     */
    public void applyNameMapping(NameMapping nameMapping) {
        final StorageMetadata storageMetadata = getMetadata(nameMapping.getModelId());
        Schema inputSchema = storageMetadata.getInputSchema();
        Schema outputSchema = storageMetadata.getOutputSchema();

        inputSchema.setNameMapping(nameMapping.getInputMapping());
        outputSchema.setNameMapping(nameMapping.getOutputMapping());
        saveMetadata(storageMetadata, nameMapping.getModelId());
    }

    /**
     * Clear a name mapping from a dataframe
     *
     * @param modelId the model for which to clear the name mappings
     */
    public void clearNameMapping(String modelId) {
        final StorageMetadata storageMetadata = getMetadata(modelId);
        Schema inputSchema = storageMetadata.getInputSchema();
        Schema outputSchema = storageMetadata.getOutputSchema();

        inputSchema.setNameMapping(new HashMap<>());
        outputSchema.setNameMapping(new HashMap<>());
        saveMetadata(storageMetadata, modelId);
    }
}
