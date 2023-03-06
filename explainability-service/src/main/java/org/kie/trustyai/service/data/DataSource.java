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
import org.kie.trustyai.service.data.exceptions.StorageReadException;
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
        } catch (JsonProcessingException e) {
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

    public void saveDataframe(Dataframe dataframe, String modelId) {

        // Add to known models
        this.knownModels.add(modelId);

        if (!storage.get().dataExists(modelId)) {
            if (!hasMetadata(modelId)) {
                final Metadata metadata = new Metadata();
                metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
                metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
                metadata.setModelId(modelId);
                try {
                    saveMetadata(metadata, modelId);
                } catch (JsonProcessingException e) {
                    throw new DataframeCreateException(e.getMessage());
                }
            }

            storage.get().saveData(parser.toByteBuffer(dataframe, false), modelId);
        } else {
            storage.get().appendData(parser.toByteBuffer(dataframe, false), modelId);
        }
    }

    public void updateMetadataObservations(int number, String modelId) throws JsonProcessingException {
        final Metadata metadata = getMetadata(modelId);
        metadata.incrementObservations(number);
        saveMetadata(metadata, modelId);
    }

    public void saveMetadata(Metadata metadata, String modelId) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final ByteBuffer byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(metadata).getBytes());
        storage.get().save(byteBuffer, modelId + "-" + METADATA_FILENAME);
    }

    public Metadata getMetadata(String modelId) throws StorageReadException, JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final ByteBuffer metadataBytes = storage.get().read(modelId + "-" + METADATA_FILENAME);
        return mapper.readValue(new String(metadataBytes.array(), StandardCharsets.UTF_8), Metadata.class);

    }

    public boolean hasMetadata(String modelId) {
        return storage.get().fileExists(modelId + "-" + METADATA_FILENAME);
    }

}
