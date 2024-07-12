package org.kie.trustyai.service.data.storage.hibernate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
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
        Dataframe df = DataframeGenerators.generateRandomNColumnDataframe(100, 5);
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
    void testClear() {
        int nrows = 100;
        int ncols = 10;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        storage.get().saveDataframe(original, MODEL_ID);

        Dataframe recovered = storage.get().readDataframe(MODEL_ID, nrows);
        DataframeGenerators.roughEqualityCheck(original, recovered);

        storage.get().clearData(MODEL_ID);
        assertFalse(storage.get().dataExists(MODEL_ID));

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
    void testColumnValueCollection() {
        int nrows = 100;
        int ncols = 10;

        Dataframe original = DataframeGenerators.generatePositionalHintedDataframe(nrows, ncols);
        storage.get().saveDataframe(original, MODEL_ID);

        assertThrows(StorageReadException.class, () -> storage.get().getColumnValues(MODEL_ID, "f-1"));
        for (int i = 0; i < original.getInputsCount(); i++) {
            assertEquals(original.getColumn(i), storage.get().getColumnValues(MODEL_ID, "f" + i));
        }
        assertEquals(original.getColumn(original.getOutputsIndices().get(0)), storage.get().getColumnValues(MODEL_ID, "o0"));
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
        final StorageMetadata storageMetadata = new StorageMetadata();

        storageMetadata.setInputSchema(MetadataUtils.getInputSchema(original));
        storageMetadata.setOutputSchema(MetadataUtils.getOutputSchema(original));
        storageMetadata.setObservations(original.getRowDimension());
        storageMetadata.setModelId(original.getId());
        storage.get().saveMetaOrInternalData(storageMetadata, MODEL_ID);

        Dataframe recovered = storage.get().readDataframeAndMetadataWithTags(MODEL_ID, nonSyntheticIdxs.size(), Set.of(Dataframe.InternalTags.UNLABELED.get())).getLeft();
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

    @Test
    void testStorageMetadataSaveLoad() {
        int nrows = 1000;
        int batch = 10;
        int ncols = 10;
        int size = nrows;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);

        HashMap<String, String> inputMapping = new HashMap<>();
        inputMapping.put("f1", "f1 Mapped");
        inputMapping.put("f2", "f2 Mapped");

        StorageMetadata sm = new StorageMetadata();
        sm.setInputSchema(MetadataUtils.getInputSchema(original));
        sm.setOutputSchema(MetadataUtils.getOutputSchema(original));
        sm.getInputSchema().setNameMapping(inputMapping);

        Set<String> originalMapping = new HashSet<>(sm.getInputSchema().getNameMappedItems().keySet());
        storage.get().saveMetaOrInternalData(sm, MODEL_ID);

        StorageMetadata smLoaded = storage.get().readMetaOrInternalData(MODEL_ID);

        assertEquals(originalMapping, new HashSet<>(smLoaded.getInputSchema().getNameMappedItems().keySet()));
    }

    @Test
    void testTagSaveLoad() {
        int nrows = 1000;
        int ncols = 10;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);

        List<String> tags = List.of("TRAINING", "SYNTHETIC");
        int idx = 0;
        HashMap<String, List<Integer>> tagIDXGroundTruth = new HashMap<>();
        HashMap<String, List<List<Integer>>> tagMap = new HashMap<>();

        for (String tag : tags) {
            tagMap.put(tag, List.of(List.of(idx, idx + 3), List.of(idx + 5, idx + 7), List.of(idx + 9)));
            tagIDXGroundTruth.put(tag, List.of(idx, idx + 1, idx + 2, idx + 5, idx + 6, idx + 9));
            idx += 10;
        }
        DataTagging dataTagging = new DataTagging(MODEL_ID, tagMap);

        storage.get().saveDataframe(original, MODEL_ID);
        storage.get().setTags(dataTagging);

        original.tagDataPoints(tagMap);
        Dataframe loaded = storage.get().readDataframe(MODEL_ID);

        assertEquals(original.getTags(), loaded.getTags());
    }
}
