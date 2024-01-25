package org.kie.trustyai.service.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.Metadata;
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

    private Map<String, String> getJointNameAliases(Metadata metadata) {
        HashMap<String, String> jointMapping = new HashMap<>();
        jointMapping.putAll(metadata.getInputSchema().getNameMapping());
        jointMapping.putAll(metadata.getOutputSchema().getNameMapping());
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
            final Metadata metadata;
            try {
                metadata = getMetadata(modelId);
            } catch (StorageReadException e) {
                throw new DataframeCreateException("Could not parse metadata: " + e.getMessage());
            }

            df = parser.toDataframe(dataByteBuffer, internalDataByteBuffer, metadata);
            df.setColumnAliases(getJointNameAliases(metadata));
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
            final Metadata metadata;
            try {
                metadata = getMetadata(modelId);
            } catch (StorageReadException e) {
                throw new DataframeCreateException("Could not parse metadata: " + e.getMessage());
            }

            df = parser.toDataframe(byteBuffer, internalDataByteBuffer, metadata);
            df.setColumnAliases(getJointNameAliases(metadata));
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
            final Metadata metadata = new Metadata();
            metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
            metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
            LOG.info("input schema:"+metadata.getInputSchema().getItems());
            LOG.info("output schema:"+metadata.getOutputSchema().getItems());
            metadata.setModelId(modelId);
            metadata.setObservations(dataframe.getRowDimension());
            try {
                saveMetadata(metadata, modelId, false);
            } catch (StorageWriteException e) {
                throw new DataframeCreateException(e.getMessage());
            }
        } else {
            // If metadata is present, just increment number of observations
            final Metadata metadata = getMetadata(modelId);

            // validate metadata
            Schema newInputSchema = MetadataUtils.getInputSchema(dataframe);
            Schema newOutputSchema = MetadataUtils.getOutputSchema(dataframe);

            if (metadata.getInputSchema().equals(newInputSchema) && metadata.getOutputSchema().equals(newOutputSchema)) {
                metadata.incrementObservations(dataframe.getRowDimension());

                // update value list
                metadata.mergeInputSchema(newInputSchema);
                metadata.mergeOutputSchema(newOutputSchema);

                try {
                    saveMetadata(metadata, modelId, true);
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
        final Metadata metadata = getMetadata(modelId);
        metadata.incrementObservations(number);
        saveMetadata(metadata, modelId, true);
    }

    public void saveMetadata(Metadata metadata, String modelId) throws StorageWriteException {
        saveMetadata(metadata, modelId, false);
    }

    public void saveMetadata(Metadata metadata, String modelId, boolean isUpdate) throws StorageWriteException {
        if (storage.get().getDataFormat() == DataFormat.CSV) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
            final ByteBuffer byteBuffer;
            try {
                byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(metadata).getBytes());
            } catch (JsonProcessingException e) {
                throw new StorageWriteException("Could not save metadata: " + e.getMessage());
            }

            ((FlatFileStorage) storage.get()).save(byteBuffer, modelId + "-" + METADATA_FILENAME);
        } else {
            if (isUpdate){
                if (!metadata.getModelId().equals(modelId)){
                    throw new IllegalArgumentException(String.format(
                            "When updating metadata record in database, the metadata's modelId must match the modelId passed to saveMetadata(). " +
                            "metadata.getModelId()=%s, modelId passed to saveMetadata()=%s",
                            metadata.getModelId(), modelId));
                }
                ((HibernateStorage) storage.get()).updateMetadata(metadata);
            } else {
                ((HibernateStorage) storage.get()).saveMetadata(metadata, modelId);
            }
        }
    }

    public Metadata getMetadata(String modelId) throws StorageReadException {
        if (storage.get().getDataFormat() == DataFormat.CSV) {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
            final ByteBuffer metadataBytes = ((FlatFileStorage) storage.get()).read(modelId + "-" + METADATA_FILENAME);
            try {
                return mapper.readValue(new String(metadataBytes.array(), StandardCharsets.UTF_8), Metadata.class);
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
