package org.kie.trustyai.service.mocks.hibernate;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.HibernateDataSource;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.utils.DataframeGenerators;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.Mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Mock
@Alternative
@ApplicationScoped
@Priority(1)
public class MockHibernateDatasource extends HibernateDataSource {

    private static final String MODEL_ID = "example1";

    public MockHibernateDatasource() {
    }

    public StorageMetadata createMetadata(Dataframe dataframe) {
        final StorageMetadata storageMetadata = new StorageMetadata();

        storageMetadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
        storageMetadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
        storageMetadata.setObservations(dataframe.getRowDimension());
        storageMetadata.setModelId(dataframe.getId());

        return storageMetadata;
    }

    public void reset() throws JsonProcessingException {
        this.knownModels.clear();
    }

    public void populate() {
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(1000);
        saveDataframe(dataframe, MODEL_ID);
        saveMetadata(createMetadata(dataframe), MODEL_ID);

    }

    @Override
    public void saveDataframe(Dataframe dataframe, String modelId) throws InvalidSchemaException {
        super.saveDataframe(dataframe, modelId);
    }
}
