package org.kie.trustyai.service.mocks.flatfile;

import java.io.File;
import java.nio.ByteBuffer;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.parsers.DataParser;
import org.kie.trustyai.service.data.storage.flatfile.PVCStorage;
import org.kie.trustyai.service.mocks.MockServiceConfig;
import org.kie.trustyai.service.mocks.MockStorageConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.Mock;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;

import static org.kie.trustyai.service.data.datasources.DataSource.INTERNAL_DATA_FILENAME;
import static org.kie.trustyai.service.data.datasources.DataSource.METADATA_FILENAME;
import static org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource.createMetadata;

@Mock
@Alternative
@ApplicationScoped
public class MockPVCStorage extends PVCStorage {
    @Inject
    DataParser parser;

    public MockPVCStorage() {
        super(new MockServiceConfig(), new MockStorageConfig());
    }

    public boolean emptyStorage(String filepath) {
        File file = new File(filepath);
        return file.delete();
    }

    // emulate the Datasource.saveDataframe method without needing a real Datasource instance
    public void emulateDatasourceSaveDataframe(Dataframe dataframe, String modelId, boolean overwrite) {
        ByteBuffer[] byteBuffers = parser.toByteBuffers(dataframe, false);
        if (!dataExists(modelId) || overwrite) {
            saveMetaOrInternalData(byteBuffers[0], modelId);
            saveDataframe(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
        } else {
            appendMetaOrInternalData(byteBuffers[0], modelId);
            append(byteBuffers[1], modelId + "-" + INTERNAL_DATA_FILENAME);
        }

        emulateDataSourceSaveMetadata(createMetadata(dataframe), modelId);
    }

    private void emulateDataSourceSaveMetadata(StorageMetadata storageMetadata, String modelId) throws StorageWriteException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT);
        final ByteBuffer byteBuffer;
        try {
            byteBuffer = ByteBuffer.wrap(mapper.writeValueAsString(storageMetadata).getBytes());
        } catch (JsonProcessingException e) {
            throw new StorageWriteException("Could not save metadata: " + e.getMessage());
        }

        saveDataframe(byteBuffer, modelId + "-" + METADATA_FILENAME);
    }

}
