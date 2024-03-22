package org.kie.trustyai.service.data;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.mocks.MockDatasource;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


abstract class DataSourceBaseTest {

    @Inject
    Instance<MockDatasource> datasource;

    public static final String MODEL_ID = "fake-model";

    @Test
    void testSavingAndReadingDataframe() {
        final Dataframe dataframe = datasource.get().generateRandomDataframe(10);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        final Dataframe readDataframe = datasource.get().getDataframe(MODEL_ID);
        assertNotNull(readDataframe);
        assertEquals(10, dataframe.getRowDimension());
        for (int i = 0; i < dataframe.getRowDimension(); i++) {
            assertEquals(dataframe.getRow(i), readDataframe.getRow(i));
        }
    }

    @Test
    void testReadingBatch() {
        final int N = 10000;
        final Random random = new Random();
        final List<String> ids = new ArrayList<>();
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0 ; i < N ; i++) {
            final PredictionInput input = new PredictionInput(List.of(FeatureFactory.newNumericalFeature("a", random.nextDouble())));
            final PredictionOutput output = new PredictionOutput(List.of(new Output("b", Type.NUMBER, new Value(random.nextDouble()), 1.0)));
            final String id = UUID.randomUUID().toString();
            ids.add(id);
            final PredictionMetadata metadata = new PredictionMetadata(id, LocalDateTime.now(), Dataframe.InternalTags.UNLABELED.get());
            final Prediction prediction = new SimplePrediction(input, output, metadata);
            predictions.add(prediction);
        }
        final Dataframe dataframe = Dataframe.createFrom(predictions);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        final Dataframe readDataframe = datasource.get().getDataframe("fake-model", 20);
        assertNotNull(readDataframe);
        assertEquals(20, readDataframe.getRowDimension());
        final List<String> readIds = readDataframe.getIds();
        final List<String> originalIds = ids.subList(ids.size() - 20, ids.size());
        assertEquals(originalIds, readIds);
    }
}
