package org.kie.trustyai.service.mocks;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
public class MockDatasource extends DataSource {

    private static final String MODEL_ID = "example1";
    @Inject
    Instance<MockMemoryStorage> storage;

    public MockDatasource() {
    }

    public Dataframe generateRandomDataframe(int observations) {
        final List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random();
        for (int i = 0; i < observations; i++) {
            final List<Feature> featureList = List.of(
                    // Metadata
                    FeatureFactory.newObjectFeature(MetadataUtils.ID_FIELD, UUID.randomUUID()),
                    FeatureFactory.newObjectFeature(MetadataUtils.TIMESTAMP_FIELD, LocalDateTime.now()),

                    FeatureFactory.newNumericalFeature("age", random.nextInt(100)),
                    FeatureFactory.newNumericalFeature("gender", random.nextBoolean() ? 1 : 0),
                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    public Metadata createMetadata(Dataframe dataframe) {
        final Metadata metadata = new Metadata();

        metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
        metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
        metadata.setObservations(dataframe.getRowDimension());

        return metadata;
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

}
