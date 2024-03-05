package org.kie.trustyai.service.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class DataSource {
    public static final String METADATA_FILENAME = "metadata.json";
    public static final String GROUND_TRUTH_SUFFIX = "-ground-truths";
    public static final String INTERNAL_DATA_FILENAME = "internal_data.csv";
    private static final Logger LOG = Logger.getLogger(DataSource.class);
    protected final Set<String> knownModels = new HashSet<>();

    @Inject
    Instance<Storage> storage;

    @Inject
    DataParser parser;
    @Inject
    ServiceConfig serviceConfig;

    public Set<String> getKnownModels() {
        return knownModels;
    }

    private Map<String, String> getJointNameAliases(StorageMetadata storageMetadata) {
        HashMap<String, String> jointMapping = new HashMap<>();
        jointMapping.putAll(storageMetadata.getInputSchema().getNameMapping());
        jointMapping.putAll(storageMetadata.getOutputSchema().getNameMapping());
        return jointMapping;
    }

    public Dataframe getDataframe(final String modelId) throws DataframeCreateException {
        Dataframe df;
        Storage st = storage.get();
        if (st.getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) st;

            final ByteBuffer dataByteBuffer;
            try {
                dataByteBuffer = ffst.readData(modelId);
            } catch (StorageReadException e) {
                throw new DataframeCreateException(e.getMessage());
            }

            final ByteBuffer internalDataByteBuffer;
            try {
                internalDataByteBuffer = ffst.read(modelId + "-" + INTERNAL_DATA_FILENAME);
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
        } else {
            HibernateStorage hst = (HibernateStorage) storage.get();
            df = hst.readData(modelId);
        }
        return df;
    }

    public Dataframe getDataframe(final String modelId, int batchSize) throws DataframeCreateException {
        Dataframe df;
        if (storage.get().getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) storage.get();

            final ByteBuffer byteBuffer;
            try {
                byteBuffer = ffst.readData(modelId, batchSize);
            } catch (StorageReadException e) {
                throw new DataframeCreateException(e.getMessage());
            }

            final ByteBuffer internalDataByteBuffer;
            try {
                internalDataByteBuffer = ffst.read(modelId + "-" + INTERNAL_DATA_FILENAME);
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
        } else {
            HibernateStorage hst = (HibernateStorage) storage.get();
            df = hst.readData(modelId, batchSize);
        }

        return df;
    }

    public void saveDataframe(final Dataframe dataframe, final String modelId) throws InvalidSchemaException {
        saveDataframe(dataframe, modelId, false);
    }

    public void saveDataframe(final Dataframe dataframe, final String modelId, boolean overwrite) throws InvalidSchemaException {
        // Add to known models
        this.knownModels.add(modelId);

        if (!hasMetadata(modelId) || overwrite) {
            // If metadata is not present, create it
            // alternatively, overwrite existing metadata if requested
            final StorageMetadata storageMetadata = new StorageMetadata();
            storageMetadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
            storageMetadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
            storageMetadata.setModelId(modelId);
            storageMetadata.setObservations(dataframe.getRowDimension());
            try {
                saveMetadata(storageMetadata, modelId, false);
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
                    saveMetadata(storageMetadata, modelId, true);
                } catch (StorageWriteException e) {
                    throw new DataframeCreateException(e.getMessage());
                }
            } else {
                final String message = "Payload schema and stored schema are not the same";
                LOG.error(message);
                throw new InvalidSchemaException(message);
            }
        }

        if (storage.get().getDataFormat() == DataFormat.CSV) {
            FlatFileStorage ffst = (FlatFileStorage) storage.get();
            ByteBuffer[] byteBuffers = parser.toByteBuffers(dataframe, false);
            if (!ffst.dataExists(modelId) || overwrite) {
                ffst.saveData(byteBuffers[0], modelId);
                ffst.save(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
            } else {
                ffst.appendData(byteBuffers[0], modelId);
                ffst.append(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
            }
        } else {
            HibernateStorage hst = (HibernateStorage) storage.get();
            if (!hst.dataframeExists(modelId) || overwrite) {
                hst.save(dataframe, modelId);
            } else {
                hst.append(dataframe, modelId);
            }
        }

    }

    public void updateMetadataObservations(int number, String modelId) {
        final StorageMetadata storageMetadata = getMetadata(modelId);
        storageMetadata.incrementObservations(number);
        saveMetadata(storageMetadata, modelId, true);
    }

    public void saveMetadata(StorageMetadata storageMetadata, String modelId) throws StorageWriteException {
        saveMetadata(storageMetadata, modelId, false);
    }

    public void saveMetadata(StorageMetadata storageMetadata, String modelId, boolean isUpdate) throws StorageWriteException {
        if (storage.get().getDataFormat() == DataFormat.CSV) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
            final ByteBuffer byteBuffer;
            try {
                byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(storageMetadata).getBytes());
            } catch (JsonProcessingException e) {
                throw new StorageWriteException("Could not save metadata: " + e.getMessage());
            }

            ((FlatFileStorage) storage.get()).save(byteBuffer, modelId + "-" + METADATA_FILENAME);
        } else {
            if (isUpdate) {
                if (!storageMetadata.getModelId().equals(modelId)) {
                    throw new IllegalArgumentException(String.format(
                            "When updating metadata record in database, the metadata's modelId must match the modelId passed to saveMetadata(). " +
                                    "metadata.getModelId()=%s, modelId passed to saveMetadata()=%s",
                            storageMetadata.getModelId(), modelId));
                }
                ((HibernateStorage) storage.get()).updateMetadata(storageMetadata);
            } else {
                ((HibernateStorage) storage.get()).saveMetadata(storageMetadata, modelId);
            }
        }
    }

    public StorageMetadata getMetadata(String modelId) throws StorageReadException {
        if (storage.get().getDataFormat() == DataFormat.CSV) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
            final ByteBuffer metadataBytes = ((FlatFileStorage) storage.get()).read(modelId + "-" + METADATA_FILENAME);
            try {
                return mapper.readValue(new String(metadataBytes.array(), StandardCharsets.UTF_8), StorageMetadata.class);
            } catch (JsonProcessingException e) {
                LOG.error("Could not parse metadata: " + e.getMessage());
                throw new StorageReadException(e.getMessage());
            }
        } else {
            return ((HibernateStorage) storage.get()).readMetadata(modelId);
        }
    }

    public void verifyKnownModels() {
        knownModels.removeIf(modelID -> !hasMetadata(modelID));
    }

    public boolean hasMetadata(String modelId) {
        if (storage.get().getDataFormat() == DataFormat.CSV) {
            return ((FlatFileStorage) storage.get()).fileExists(modelId + "-" + METADATA_FILENAME);
        } else {
            return ((HibernateStorage) storage.get()).dataframeExists(modelId);
        }
    }

    public static String getGroundTruthName(String modelId) {
        return modelId + GROUND_TRUTH_SUFFIX;
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

}
