package org.kie.trustyai.service.mocks;

import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.Mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.kie.trustyai.service.utils.DataframeGenerators.generateRandomDataframe;

@Mock
@Alternative
@ApplicationScoped
@Priority(1)
public class MockDatasource extends DataSource {

    private static final String MODEL_ID = "example1";
    @Inject
    Instance<MockMemoryStorage> storage;

    public MockDatasource() {
    }

    public StorageMetadata createMetadata(Dataframe dataframe) {
        final StorageMetadata storageMetadata = new StorageMetadata();

        storageMetadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
        storageMetadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
        storageMetadata.setObservations(dataframe.getRowDimension());

        return storageMetadata;
    }

    public void reset() throws JsonProcessingException {
        this.empty();
        final Dataframe dataframe = generateRandomDataframe(1000);
        saveDataframe(dataframe, MODEL_ID);
        saveMetadata(createMetadata(dataframe), MODEL_ID);
    }

    public void empty() {
        storage.get().emptyStorage();
        this.knownModels.clear();
    }

    @Override
    public void saveDataframe(Dataframe dataframe, String modelId) throws InvalidSchemaException {
        super.saveDataframe(dataframe, modelId);
    }
}
