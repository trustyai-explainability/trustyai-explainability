package org.kie.trustyai.service.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Singleton
public class DataSource {
    public static final String METADATA_FILENAME = "metadata.json";
    private static final Logger LOG = Logger.getLogger(DataSource.class);
    @Inject
    Instance<Storage> storage;
    @Inject
    DataParser parser;
    @Inject
    ServiceConfig serviceConfig;

    private Metadata metadata = null;

    public Dataframe getDataframe() throws DataframeCreateException {

        final ByteBuffer byteBuffer;
        try {
            byteBuffer = storage.get().getData();
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }

        // Fetch metadata, if not yet read
        if (this.metadata == null) {
            try {
                this.metadata = getMetadata();
            } catch (JsonProcessingException e) {
                throw new DataframeCreateException("Could not parse metadata: " + e.getMessage());
            }
        }

        final Dataframe dataframe = parser.toDataframe(byteBuffer, this.metadata);

        if (serviceConfig.batchSize().isPresent()) {
            final int batchSize = serviceConfig.batchSize().getAsInt();
            LOG.info("Batching with " + batchSize + " rows. Passing " + dataframe.getRowDimension() + " rows");
            return dataframe;
        } else {
            LOG.info("No batching. Passing all of " + dataframe.getRowDimension() + " rows");
            return dataframe;
        }
    }

    public void saveDataframe(Dataframe dataframe) {
        if (!storage.get().dataExists()) {
            storage.get().saveData(parser.toByteBuffer(dataframe, false));
        } else {
            storage.get().appendData(parser.toByteBuffer(dataframe, false));
        }
    }

    public void saveMetadata(Metadata metadata) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final ByteBuffer byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(metadata).getBytes());
        storage.get().save(byteBuffer, METADATA_FILENAME);
    }

    public Metadata getMetadata() throws StorageReadException, JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        final ByteBuffer metadataBytes = storage.get().read(METADATA_FILENAME);
        return mapper.readValue(new String(metadataBytes.array(), StandardCharsets.UTF_8), Metadata.class);

    }

    public boolean hasMetadata() {
        return storage.get().fileExists(METADATA_FILENAME);
    }

}
