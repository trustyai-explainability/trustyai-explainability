package org.kie.trustyai.service.data.storage.hibernate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
class HibernateStorageTest {
    @Inject
    Instance<MockHibernateStorage> storage;

    String MODEL_ID = "example_model";

    @BeforeEach
    void emptyStorage() {
        storage.get().clearData(MODEL_ID);
    }

    @Test
    void testWrite() {
        Dataframe df = DataframeGenerators.generateRandomNColumnDataframe(100, 10);
        storage.get().saveDataframe(df, MODEL_ID);
        assertTrue(storage.get().dataExists(MODEL_ID));
    }

    @Test
    void testRead() {
        int nrows = 100;
        int ncols = 10;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        storage.get().saveDataframe(original, MODEL_ID);
        Dataframe recovered = storage.get().readDataframe(MODEL_ID, nrows);
        DataframeGenerators.roughEqualityCheck(original, recovered);
    }

    @Test
    void testCounts() {
        int nrows = 100;
        int ncols = 10;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        storage.get().saveDataframe(original, MODEL_ID);
        assertEquals(nrows, storage.get().rowCount(MODEL_ID));
        assertEquals(original.getColumnDimension(), storage.get().colCount(MODEL_ID));
    }

    @Test
    void testBatchedRead() {
        int nrows = 100;
        int ncols = 10;
        int batch = 10;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        Dataframe batched = original.filterByRowIndex(IntStream.range(nrows - batch, nrows).boxed().collect(Collectors.toList()));

        storage.get().saveDataframe(original, MODEL_ID);
        Dataframe recovered = storage.get().readDataframe(MODEL_ID, batch);
        DataframeGenerators.roughEqualityCheck(batched, recovered);
    }

    @Test
    void testSlicedRead() {
        int nrows = 10;
        int ncols = 10;
        int startPos = 5;
        int endPos = 8;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        Dataframe batched = original.filterByRowIndex(IntStream.range(startPos, endPos).boxed().collect(Collectors.toList()));

        storage.get().saveDataframe(original, MODEL_ID);
        Dataframe recovered = storage.get().readDataframe(MODEL_ID, startPos, endPos);
        DataframeGenerators.roughEqualityCheck(batched, recovered);
    }

    @Test
    void testNonSyntheticRead() {
        int nrows = 100;
        int ncols = 10;

        // alternate synthetic/nonsynthetic data every $freq rows
        int syntheticFreq = 5;

        Dataframe original = DataframeGenerators.generatePositionalHintedDataframe(nrows, ncols);
        Map<String, List<List<Integer>>> tagMap = new HashMap<>();
        List<Integer> nonSyntheticIdxs = new ArrayList<>();
        List<List<Integer>> syntheticSlices = new ArrayList<>();
        for (int i = 0; i < nrows; i += 2 * syntheticFreq) {
            syntheticSlices.add(List.of(i, i + syntheticFreq));
            nonSyntheticIdxs.addAll(IntStream.range(i + syntheticFreq, i + 2 * syntheticFreq).boxed().collect(Collectors.toList()));
        }
        tagMap.put(Dataframe.InternalTags.SYNTHETIC.get(), syntheticSlices);

        original.tagDataPoints(tagMap);
        storage.get().saveDataframe(original, MODEL_ID);

        Dataframe nonSynthetic = original.filterByRowIndex(nonSyntheticIdxs);
        Dataframe recovered = storage.get().readNonSyntheticDataframe(MODEL_ID, nonSyntheticIdxs.size());
        DataframeGenerators.roughEqualityCheck(nonSynthetic, recovered);
    }

    @Test
    void testBatchedWrite() {
        int nrows = 1000;
        int batch = 10;
        int ncols = 10;
        int size = nrows;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        storage.get().saveDataframe(original, MODEL_ID);

        for (int i = 0; i < 5; i++) {
            Dataframe appendee = DataframeGenerators.generateRandomNColumnDataframe(batch, ncols);
            original.addPredictions(appendee.asPredictions());
            storage.get().append(appendee, MODEL_ID);
            size += batch;
        }

        assertEquals(size, storage.get().rowCount(MODEL_ID));
        DataframeGenerators.roughEqualityCheck(original, storage.get().readDataframe(MODEL_ID));
    }

}
