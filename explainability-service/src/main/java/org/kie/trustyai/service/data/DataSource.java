package org.kie.trustyai.service.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.parsers.DataParser;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.payloads.service.Schema;
import org.kie.trustyai.service.payloads.values.DataType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        final ByteBuffer dataByteBuffer;
        try {
            dataByteBuffer = storage.get().readData(modelId);
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final ByteBuffer internalDataByteBuffer;
        try {
            internalDataByteBuffer = storage.get().read(modelId + "-" + INTERNAL_DATA_FILENAME);
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

        Dataframe df = parser.toDataframe(dataByteBuffer, internalDataByteBuffer, metadata);
        df.setColumnAliases(getJointNameAliases(metadata));
        return df;
    }

    public Dataframe getDataframe(final String modelId, int batchSize) throws DataframeCreateException {
        final ByteBuffer byteBuffer;
        try {
            byteBuffer = storage.get().readData(modelId, batchSize);
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        final ByteBuffer internalDataByteBuffer;
        try {
            internalDataByteBuffer = storage.get().read(modelId + "-" + INTERNAL_DATA_FILENAME);
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

        Dataframe df = parser.toDataframe(byteBuffer, internalDataByteBuffer, metadata);
        df.setColumnAliases(getJointNameAliases(metadata));
        return df;
    }

    public void saveDataframe(final Dataframe dataframe, final String modelId) throws InvalidSchemaException {
        saveDataframe(dataframe, modelId, false);
    }

    public void saveDataframe(final Dataframe dataframe, final String modelId, boolean overwrite) throws InvalidSchemaException {
        saveDataframe(dataframe, modelId, null, overwrite);
    }

    public void saveDataframe(final Dataframe dataframe, final String modelId, List<DataType> types) throws InvalidSchemaException {
        saveDataframe(dataframe, modelId, types, false);
    }

    public synchronized void saveDataframe(final Dataframe dataframe, final String modelId, @Nullable List<DataType> types, boolean overwrite) throws InvalidSchemaException {
        // Add to known models
        this.knownModels.add(modelId);

        if (!hasMetadata(modelId) || overwrite) {
            // If metadata is not present, create it
            // alternatively, overwrite existing metadata if requested
            final Metadata metadata = new Metadata();
            metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe, types));
            metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe, types));
            metadata.setModelId(modelId);
            metadata.setObservations(dataframe.getRowDimension());
            try {
                saveMetadata(metadata, modelId);
            } catch (StorageWriteException e) {
                throw new DataframeCreateException(e.getMessage());
            }
        } else {
            // If metadata is present, just increment number of observations
            final Metadata metadata = getMetadata(modelId);

            // validate metadata
            Schema newInputSchema = MetadataUtils.getInputSchema(dataframe, types);
            Schema newOutputSchema = MetadataUtils.getOutputSchema(dataframe, types);

            if (metadata.getInputSchema().equals(newInputSchema) && metadata.getOutputSchema().equals(newOutputSchema)) {
                metadata.incrementObservations(dataframe.getRowDimension());

                // update value list
                metadata.mergeInputSchema(newInputSchema);
                metadata.mergeOutputSchema(newOutputSchema);

                try {
                    saveMetadata(metadata, modelId);
                } catch (StorageWriteException e) {
                    throw new DataframeCreateException(e.getMessage());
                }
            } else {
                final String message = "Payload schema and stored schema are not the same";
                LOG.error(message);
                throw new InvalidSchemaException(message);
            }
        }

        ByteBuffer[] byteBuffers = parser.toByteBuffers(dataframe, false);
        if (!storage.get().dataExists(modelId) || overwrite) {
            storage.get().saveData(byteBuffers[0], modelId);
            storage.get().save(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
        } else {
            storage.get().appendData(byteBuffers[0], modelId);
            storage.get().append(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
        }

    }

    public void updateMetadataObservations(int number, String modelId) {
        final Metadata metadata = getMetadata(modelId);
        metadata.incrementObservations(number);
        saveMetadata(metadata, modelId);
    }

    public void saveMetadata(Metadata metadata, String modelId) throws StorageWriteException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        final ByteBuffer byteBuffer;
        try {
            byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(metadata).getBytes());
        } catch (JsonProcessingException e) {
            throw new StorageWriteException("Could not save metadata: " + e.getMessage());
        }

        storage.get().save(byteBuffer, modelId + "-" + METADATA_FILENAME);
    }

    public Metadata getMetadata(String modelId) throws StorageReadException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        final ByteBuffer metadataBytes = storage.get().read(modelId + "-" + METADATA_FILENAME);
        try {
            return mapper.readValue(new String(metadataBytes.array(), StandardCharsets.UTF_8), Metadata.class);
        } catch (JsonProcessingException e) {
            LOG.error("Could not parse metadata: " + e.getMessage());
            throw new StorageReadException(e.getMessage());
        }

    }

    public void verifyKnownModels() {
        knownModels.removeIf(modelID -> !hasMetadata(modelID));
    }

    public boolean hasMetadata(String modelId) {
        return storage.get().fileExists(modelId + "-" + METADATA_FILENAME);
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
