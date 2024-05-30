package org.kie.trustyai.service.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.parsers.DataParser;
import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.data.storage.flatfile.FlatFileStorage;
import org.kie.trustyai.service.data.storage.hibernate.HibernateStorage;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.payloads.service.Schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.vertx.core.impl.ConcurrentHashSet;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DataSource {
    public static final String METADATA_FILENAME = "metadata.json";
    public static final String GROUND_TRUTH_SUFFIX = "-ground-truths";
    public static final String INTERNAL_DATA_FILENAME = "internal_data.csv";
    private static final Logger LOG = Logger.getLogger(DataSource.class);
    protected final Set<String> knownModels = new ConcurrentHashSet<>();

    @Inject
    Instance<Storage<?, ?>> storage;

    Optional<Storage<?, ?>> storageOverride = Optional.empty();

    @Inject
    DataParser parser;
    @Inject
    ServiceConfig serviceConfig;

    public void setParser(DataParser parser) {
        this.parser = parser;
    }

    public void setStorageOverride(Storage storage) {
        this.storageOverride = Optional.of(storage);
    }

    public void clearStorageOverride() {
        this.storageOverride = Optional.empty();
    }

    private Storage getStorage() {
        if (storageOverride.isPresent()) {
            LOG.debug("Using overridden storage");
            return storageOverride.get();
        } else {
            return storage.get();
        }
    }

    public static String getGroundTruthName(String modelId) {
        return modelId + GROUND_TRUTH_SUFFIX;
    }

    public Set<String> getKnownModels() {
        return knownModels;
    }

    public void addModelToKnown(String modelId) {
        this.knownModels.add(modelId);
    }

    private Map<String, String> getJointNameAliases(StorageMetadata storageMetadata) {
        HashMap<String, String> jointMapping = new HashMap<>();
        jointMapping.putAll(storageMetadata.getInputSchema().getNameMapping());
        jointMapping.putAll(storageMetadata.getOutputSchema().getNameMapping());
        return jointMapping;
    }

    public Dataframe getDataframe(final String modelId) throws DataframeCreateException {
        Dataframe df;
        Storage st = getStorage();
        if (st.getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) st;

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

            df = parser.toDataframe(dataByteBuffer, internalDataByteBuffer, storageMetadata);
            df.setColumnAliases(getJointNameAliases(storageMetadata));
            df.setInputTensorName(storageMetadata.getInputTensorName());
            df.setOutputTensorName(storageMetadata.getOutputTensorName());
        } else {
            HibernateStorage hst = (HibernateStorage) getStorage();
            df = hst.readDataframe(modelId);
        }
        return df;
    }

    public Dataframe getDataframe(final String modelId, int batchSize) throws DataframeCreateException {
        Dataframe df;
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) getStorage();

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

            df = parser.toDataframe(byteBuffer, internalDataByteBuffer, storageMetadata);
            df.setColumnAliases(getJointNameAliases(storageMetadata));
            df.setInputTensorName(storageMetadata.getInputTensorName());
            df.setOutputTensorName(storageMetadata.getOutputTensorName());
        } else {
            HibernateStorage hst = (HibernateStorage) getStorage();
            df = hst.readDataframe(modelId, batchSize);
        }

        return df;
    }

    public Dataframe getDataframe(final String modelId, int startPos, int endPos) throws DataframeCreateException {
        if (endPos <= startPos) {
            throw new IllegalArgumentException("DataSource.getDataframe endPos must be greater than startPos. Got startPos=" + startPos + ", endPos=" + endPos);
        }

        Dataframe df;
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) getStorage();

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

            df = parser.toDataframe(byteBuffer, internalDataByteBuffer, storageMetadata);
            df.setColumnAliases(getJointNameAliases(storageMetadata));
            df.setInputTensorName(storageMetadata.getInputTensorName());
            df.setOutputTensorName(storageMetadata.getOutputTensorName());
        } else {
            HibernateStorage hst = (HibernateStorage) getStorage();
            df = hst.readDataframe(modelId, startPos, endPos);
        }
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
        Dataframe df;
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) getStorage();
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

            try {
                df = parser.toDataframe(pair.getLeft(), pair.getRight(), metadata);
            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage());
                throw new DataframeCreateException("Could not parse create dataframe: " + e.getMessage());
            }

            df.setColumnAliases(getJointNameAliases(metadata));
            df.setInputTensorName(metadata.getInputTensorName());
            df.setOutputTensorName(metadata.getOutputTensorName());
        } else {
            HibernateStorage hst = (HibernateStorage) getStorage();
            df = hst.readDataframeAndMetadataWithTags(modelId, batchSize, Set.of(Dataframe.InternalTags.UNLABELED.get())).getLeft();
        }
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
        Dataframe df;
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) getStorage();
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

            try {
                df = parser.toDataframe(pair.getLeft(), pair.getRight(), metadata);
            } catch (IllegalArgumentException e) {
                LOG.error(e.getMessage());
                throw new DataframeCreateException("Could not parse create dataframe: " + e.getMessage());
            }

            df.setColumnAliases(getJointNameAliases(metadata));
            df.setInputTensorName(metadata.getInputTensorName());
            df.setOutputTensorName(metadata.getOutputTensorName());
        } else {
            HibernateStorage hst = (HibernateStorage) getStorage();
            df = hst.readDataframeAndMetadataWithTags(modelId, Set.of(Dataframe.InternalTags.UNLABELED.get())).getLeft();
        }
        return df;
    }

    public void saveDataframe(final Dataframe dataframe, final String modelId) throws InvalidSchemaException {
        saveDataframe(dataframe, modelId, false);
    }

    public void saveDataframe(final Dataframe dataframe, final String modelId, boolean overwrite) throws InvalidSchemaException {
        // Add to known models
        addModelToKnown(modelId);

        if (!hasMetadata(modelId) || overwrite) {
            // If metadata is not present, create it
            // alternatively, overwrite existing metadata if requested

            final StorageMetadata storageMetadata = new StorageMetadata();
            storageMetadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
            storageMetadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
            storageMetadata.setModelId(modelId);
            storageMetadata.setObservations(dataframe.getRowDimension());
            storageMetadata.setInputTensorName(dataframe.getInputTensorName());
            storageMetadata.setOutputTensorName(dataframe.getOutputTensorName());
            try {
                saveMetadata(storageMetadata, modelId);
            } catch (StorageWriteException e) {
                throw new DataframeCreateException(e.getMessage());
            }
        } else {
            // If metadata is present, just increment number of observations
            final StorageMetadata storageMetadata = getMetadata(modelId);

            // validate metadata
            Schema newInputSchema = MetadataUtils.getInputSchema(dataframe);
            Schema newOutputSchema = MetadataUtils.getOutputSchema(dataframe);

            if (storageMetadata.getInputSchema().equals(newInputSchema) && storageMetadata.getOutputSchema().equals(newOutputSchema)) {
                storageMetadata.incrementObservations(dataframe.getRowDimension());

                // update value list
                storageMetadata.mergeInputSchema(newInputSchema);
                storageMetadata.mergeOutputSchema(newOutputSchema);

                try {
                    saveMetadata(storageMetadata, modelId);
                } catch (StorageWriteException e) {
                    throw new DataframeCreateException(e.getMessage());
                }
            } else {
                final String message = "Payload schema and stored schema are not the same";
                LOG.error(message);
                throw new InvalidSchemaException(message);
            }
        }

        if (getStorage().getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) getStorage();
            ByteBuffer[] byteBuffers = parser.toByteBuffers(dataframe, false);
            if (!ffst.dataExists(modelId) || overwrite) {
                ffst.saveMetaOrInternalData(byteBuffers[0], modelId);
                ffst.saveDataframe(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
            } else {
                ffst.appendMetaOrInternalData(byteBuffers[0], modelId);
                ffst.append(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
            }
        } else {
            HibernateStorage hst = (HibernateStorage) getStorage();
            if (!hst.dataExists(modelId)) {
                hst.saveDataframe(dataframe, modelId);
            } else if (overwrite) {
                hst.overwriteDataframe(dataframe, modelId);
            } else {
                hst.append(dataframe, modelId);
            }
        }

    }

    public void updateMetadataObservations(int number, String modelId) {
        final StorageMetadata storageMetadata = getMetadata(modelId);
        storageMetadata.incrementObservations(number);
        saveMetadata(storageMetadata, modelId);
    }

    public void saveMetadata(StorageMetadata storageMetadata, String modelId) throws StorageWriteException {
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
            final ByteBuffer byteBuffer;
            try {
                byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(storageMetadata).getBytes());
            } catch (JsonProcessingException e) {
                throw new StorageWriteException("Could not save metadata: " + e.getMessage());
            }

            ((FlatFileStorage) getStorage()).saveDataframe(byteBuffer, modelId + "-" + METADATA_FILENAME);
        } else {
            ((HibernateStorage) getStorage()).saveMetaOrInternalData(storageMetadata, modelId);
        }
    }

    public StorageMetadata getMetadata(String modelId) throws StorageReadException {
        return getMetadata(modelId, false);
    }

    public StorageMetadata getMetadata(String modelId, boolean loadColumnValues) throws StorageReadException {
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
            final ByteBuffer metadataBytes = ((FlatFileStorage) getStorage()).readMetaOrInternalData(modelId + "-" + METADATA_FILENAME);
            try {
                return mapper.readValue(new String(metadataBytes.array(), StandardCharsets.UTF_8), StorageMetadata.class);
            } catch (JsonProcessingException e) {
                LOG.error("Could not parse metadata: " + e.getMessage());
                throw new StorageReadException(e.getMessage());
            }
        } else {
            HibernateStorage hibernateStorage = (HibernateStorage) getStorage();
            StorageMetadata sm = hibernateStorage.readMetaOrInternalData(modelId);

            // only grab column enumerations from DB if explicitly requested, to save time
            if (loadColumnValues) {
                hibernateStorage.loadColumnValues(modelId, sm);
            }
            return sm;
        }
    }

    // shortcut the observation count for the DB case, to avoid loading all metadata unnecessarily
    public long getNumObservations(String modelId) {
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            return getMetadata(modelId).getObservations();
        } else {
            return ((HibernateStorage) getStorage()).rowCount(modelId);
        }
    }

    public List<String> getVerifiedModels() {
        return knownModels.stream().filter(this::hasMetadata).collect(Collectors.toList());
    }

    public boolean hasMetadata(String modelId) {
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            return ((FlatFileStorage) getStorage()).fileExists(modelId + "-" + METADATA_FILENAME);
        } else {
            boolean dataExists = ((HibernateStorage) getStorage()).dataExists(modelId);
            return dataExists;
        }
    }

    // ground truth access and settors
    public boolean hasGroundTruths(String modelId) {
        return hasMetadata(getGroundTruthName(modelId));
    }

    public void saveGroundTruths(Dataframe groundTruthsDataframe, String modelId) {
        saveDataframe(groundTruthsDataframe, getGroundTruthName(modelId));
    }

    public Dataframe getGroundTruths(String modelId) {
        return getDataframe(getGroundTruthName(modelId));
    }

    // tagging handler
    public void tagDataframeRows(String modelId, HashMap<String, List<List<Integer>>> tagMapping) {
        if (getStorage().getDataFormat() == DataFormat.CSV) {
            Dataframe df = getDataframe(modelId);
            df.tagDataPoints(tagMapping);
            saveDataframe(df, modelId, true);
        } else {
            ((HibernateStorage) getStorage()).setTags(modelId, tagMapping);
        }
    }

}
