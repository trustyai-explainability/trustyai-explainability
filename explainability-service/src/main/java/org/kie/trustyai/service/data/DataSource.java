package org.kie.trustyai.service.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
public class DataSource {
    public static final String METADATA_FILENAME = "metadata.json";
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

    public Dataframe getDataframe(final String modelId) throws DataframeCreateException {

        final ByteBuffer byteBuffer;
        try {
            byteBuffer = storage.get().readData(modelId);
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

        final Dataframe dataframe = parser.toDataframe(byteBuffer, metadata);

        if (serviceConfig.batchSize().isPresent()) {
            final int batchSize = serviceConfig.batchSize().getAsInt();
            LOG.info("Batching with " + batchSize + " rows. Passing " + dataframe.getRowDimension() + " rows");
            return dataframe;
        } else {
            LOG.info("No batching. Passing all of " + dataframe.getRowDimension() + " rows");
            return dataframe;
        }
    }

    public void saveDataframe(final Dataframe dataframe, final String modelId) throws InvalidSchemaException {

        // Add to known models
        this.knownModels.add(modelId);

        if (!hasMetadata(modelId)) {
            // If metadata is not present, create it
            final Metadata metadata = new Metadata();
            metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
            metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
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
            if (metadata.getInputSchema().equals(MetadataUtils.getInputSchema(dataframe)) && metadata.getOutputSchema().equals(MetadataUtils.getOutputSchema(dataframe))) {
                metadata.incrementObservations(dataframe.getRowDimension());
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

        if (!storage.get().dataExists(modelId)) {
            storage.get().saveData(parser.toByteBuffer(dataframe, false), modelId);
        } else {
            storage.get().appendData(parser.toByteBuffer(dataframe, false), modelId);
        }

    }

    public void updateMetadataObservations(int number, String modelId) {
        final Metadata metadata = getMetadata(modelId);
        metadata.incrementObservations(number);
        saveMetadata(metadata, modelId);
    }

    public void saveMetadata(Metadata metadata, String modelId) throws StorageWriteException {
        final ObjectMapper mapper = new ObjectMapper();
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
        final ByteBuffer metadataBytes = storage.get().read(modelId + "-" + METADATA_FILENAME);
        try {
            return mapper.readValue(new String(metadataBytes.array(), StandardCharsets.UTF_8), Metadata.class);
        } catch (JsonProcessingException e) {
            LOG.error("Could not parse metadata: " + e.getMessage());
            throw new StorageReadException(e.getMessage());
        }

    }

    public boolean hasMetadata(String modelId) {
        return storage.get().fileExists(modelId + "-" + METADATA_FILENAME);
    }

}
