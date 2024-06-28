package org.kie.trustyai.service.data.datasources;

import java.time.LocalDateTime;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.utils.DataframeGenerators;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

abstract class DataSourceBaseTest {

    public static final String MODEL_ID = "fake-model";
    @Inject
    Instance<DataSource> datasource;

    @Test
    void testSavingAndReadingDataframe() {
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(10);
        datasource.get().saveDataframe(dataframe, MODEL_ID);
        final Dataframe readDataframe = datasource.get().getDataframe(MODEL_ID);
        assertNotNull(readDataframe);
        assertEquals(10, dataframe.getRowDimension());
        for (int i = 0; i < dataframe.getRowDimension(); i++) {
            assertEquals(dataframe.getRow(i), readDataframe.getRow(i));
        }
    }

    @Test
    @DisplayName("Ensure internals ids are batched when reading batch data")
    void testReadingBatch() {
        final int N = 10000;
        final Random random = new Random();
        final List<String> ids = new ArrayList<>();
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < N; i++) {
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

        final int BATCH_SIZE = random.nextInt(1000) + 1;

        final Dataframe readDataframe = datasource.get().getDataframe("fake-model", BATCH_SIZE);
        assertNotNull(readDataframe);

        assertEquals(BATCH_SIZE, readDataframe.getRowDimension());
        final List<String> readIds = readDataframe.getIds();
        final List<String> originalIds = ids.subList(ids.size() - BATCH_SIZE, ids.size());
        assertEquals(originalIds, readIds);
    }

    @Test
    @DisplayName("Read with batch larger than the number of elements")
    void testReadingBatchLarger() {
        final int N = 100;
        final Random random = new Random();
        final List<String> ids = new ArrayList<>();
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < N; i++) {
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

        final int BATCH_SIZE = random.nextInt(1000) + 101;

        final Dataframe readDataframe = datasource.get().getDataframe("fake-model", BATCH_SIZE);
        assertNotNull(readDataframe);

        assertEquals(N, readDataframe.getRowDimension());
        final List<String> readIds = readDataframe.getIds();
        final List<String> originalIds = ids.subList(ids.size() - N, ids.size());
        assertEquals(originalIds, readIds);
    }

    @Test
    @DisplayName("Reading an organic batch should account for synthetic data")
    void testReadingBatchMixSyntheticOrganic() {
        final Random random = new Random();
        final int N1 = random.nextInt(300) + 100;
        final int N2 = random.nextInt(400);
        final int N3 = random.nextInt(100);
        final List<String> organicIds = new ArrayList<>();
        final List<String> syntheticIds = new ArrayList<>();
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < N1; i++) {
            final PredictionInput input = new PredictionInput(List.of(FeatureFactory.newNumericalFeature("a", random.nextDouble())));
            final PredictionOutput output = new PredictionOutput(List.of(new Output("b", Type.NUMBER, new Value(random.nextDouble()), 1.0)));
            final String id = UUID.randomUUID().toString();
            organicIds.add(id);
            final PredictionMetadata metadata = new PredictionMetadata(id, LocalDateTime.now(), Dataframe.InternalTags.UNLABELED.get());
            final Prediction prediction = new SimplePrediction(input, output, metadata);
            predictions.add(prediction);
        }
        for (int i = 0; i < N2; i++) {
            final PredictionInput input = new PredictionInput(List.of(FeatureFactory.newNumericalFeature("a", random.nextDouble())));
            final PredictionOutput output = new PredictionOutput(List.of(new Output("b", Type.NUMBER, new Value(random.nextDouble()), 1.0)));
            final String id = UUID.randomUUID().toString();
            syntheticIds.add(id);
            final PredictionMetadata metadata = new PredictionMetadata(id, LocalDateTime.now(), Dataframe.InternalTags.SYNTHETIC.get());
            final Prediction prediction = new SimplePrediction(input, output, metadata);
            predictions.add(prediction);
        }
        for (int i = 0; i < N3; i++) {
            final PredictionInput input = new PredictionInput(List.of(FeatureFactory.newNumericalFeature("a", random.nextDouble())));
            final PredictionOutput output = new PredictionOutput(List.of(new Output("b", Type.NUMBER, new Value(random.nextDouble()), 1.0)));
            final String id = UUID.randomUUID().toString();
            organicIds.add(id);
            final PredictionMetadata metadata = new PredictionMetadata(id, LocalDateTime.now(), Dataframe.InternalTags.UNLABELED.get());
            final Prediction prediction = new SimplePrediction(input, output, metadata);
            predictions.add(prediction);
        }
        final Dataframe dataframe = Dataframe.createFrom(predictions);
        datasource.get().saveDataframe(dataframe, MODEL_ID);

        // Get a batch slightly larger than N3. This forces the batch to include data from N1, since N2 is synthetic
        final int BATCH_SIZE = N3 + 10;

        final Dataframe readDataframe = datasource.get().getOrganicDataframe("fake-model", BATCH_SIZE);
        assertNotNull(readDataframe);

        assertEquals(BATCH_SIZE, readDataframe.getRowDimension());
        final Set<String> tags = readDataframe.getTags().stream().collect(Collectors.toUnmodifiableSet());
        assertEquals(1, tags.size());
        assertEquals(Dataframe.InternalTags.UNLABELED.get(), tags.iterator().next());
        assertEquals(organicIds.subList(organicIds.size() - BATCH_SIZE, organicIds.size()), readDataframe.getIds());
    }

    @Test
    @DisplayName("Reading an organic batch should account for synthetic data (no organic data)")
    void testReadingBatchMixSyntheticOrganicNoOrganic() {
        final Random random = new Random();
        final int N1 = random.nextInt(300) + 100;
        final List<String> syntheticIds = new ArrayList<>();
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < N1; i++) {
            final PredictionInput input = new PredictionInput(List.of(FeatureFactory.newNumericalFeature("a", random.nextDouble())));
            final PredictionOutput output = new PredictionOutput(List.of(new Output("b", Type.NUMBER, new Value(random.nextDouble()), 1.0)));
            final String id = UUID.randomUUID().toString();
            syntheticIds.add(id);
            final PredictionMetadata metadata = new PredictionMetadata(id, LocalDateTime.now(), Dataframe.InternalTags.SYNTHETIC.get());
            final Prediction prediction = new SimplePrediction(input, output, metadata);
            predictions.add(prediction);
        }

        final Dataframe dataframe = Dataframe.createFrom(predictions);
        datasource.get().saveDataframe(dataframe, MODEL_ID);

        // Get a batch slightly smaller than N3 (guaranteed positive)
        final int BATCH_SIZE = N1 - 10;

        assertThrows(DataframeCreateException.class, () -> {
            datasource.get().getOrganicDataframe("fake-model", BATCH_SIZE);
        }, "Cannot create a dataframe from an empty list of predictions.");
    }

    @Test
    @DisplayName("Reading an organic batch should account for synthetic data (no synthetic data)")
    void testReadingBatchMixSyntheticOrganicNoSynthetic() {
        final Random random = new Random();
        final int N1 = random.nextInt(300) + 100;
        final List<String> organicIds = new ArrayList<>();
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < N1; i++) {
            final PredictionInput input = new PredictionInput(List.of(FeatureFactory.newNumericalFeature("a", random.nextDouble())));
            final PredictionOutput output = new PredictionOutput(List.of(new Output("b", Type.NUMBER, new Value(random.nextDouble()), 1.0)));
            final String id = UUID.randomUUID().toString();
            organicIds.add(id);
            final PredictionMetadata metadata = new PredictionMetadata(id, LocalDateTime.now(), Dataframe.InternalTags.UNLABELED.get());
            final Prediction prediction = new SimplePrediction(input, output, metadata);
            predictions.add(prediction);
        }

        final Dataframe dataframe = Dataframe.createFrom(predictions);
        datasource.get().saveDataframe(dataframe, MODEL_ID);

        // Get a batch slightly smaller than N3 (guaranteed positive)
        final int BATCH_SIZE = N1 - 10;

        final Dataframe readDataframe = datasource.get().getOrganicDataframe("fake-model", BATCH_SIZE);
        assertNotNull(readDataframe);

        assertEquals(BATCH_SIZE, readDataframe.getRowDimension());
        final Set<String> tags = readDataframe.getTags().stream().collect(Collectors.toUnmodifiableSet());
        assertEquals(1, tags.size());
        assertEquals(Dataframe.InternalTags.UNLABELED.get(), tags.iterator().next());
        assertEquals(organicIds.subList(organicIds.size() - BATCH_SIZE, organicIds.size()), readDataframe.getIds());
    }

    @Test
    @DisplayName("Reading an organic batch should account for synthetic data (no synthetic data)")
    void testReadingOrganicBatchesOverTime() {
        Dataframe df;
        for (int i = 0; i < 10; i++) {
            df = DataframeGenerators.generateRandomDataframe(50);
            datasource.get().saveDataframe(df, MODEL_ID);
            final Dataframe readDataframe = datasource.get().getOrganicDataframe(MODEL_ID, 50);
        }
    }

    @Test
    @DisplayName("Asking for the tags of a non-existant model should raise an error")
    void testGetTagsOfUnknownModel() {
        Dataframe df = DataframeGenerators.generateRandomDataframe(50);
        datasource.get().saveDataframe(df, MODEL_ID);

        assertThrows(StorageReadException.class, () -> datasource.get().getTags("nonexistant"));
    }

    @Test
    void testReadingOrganicNoData() throws InterruptedException {
        int nrows = 100;
        int ncols = 100;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        HashMap<String, List<List<Integer>>> tagMap = new HashMap<>();
        tagMap.put("TRAINING", List.of(List.of(0, nrows)));
        DataTagging dataTagging = new DataTagging(MODEL_ID, tagMap);
        datasource.get().saveDataframe(original, MODEL_ID);
        datasource.get().tagDataframeRows(dataTagging);
        assertThrows(DataframeCreateException.class, () -> datasource.get().getOrganicDataframe(MODEL_ID));
    }

    @Test
    void testReadingTagFiltering() {
        int nrows = 100;
        int ncols = 10;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        HashMap<String, List<List<Integer>>> tagMap = new HashMap<>();
        tagMap.put("A", List.of(List.of(0, 10)));
        tagMap.put("B", List.of(List.of(20, 30)));
        tagMap.put("C", List.of(List.of(40, 50)));

        DataTagging dataTagging = new DataTagging(MODEL_ID, tagMap);
        datasource.get().saveDataframe(original, MODEL_ID);
        datasource.get().tagDataframeRows(dataTagging);

        // not tags
        Dataframe df = datasource.get().getDataframeFilteredByNotTags(MODEL_ID, Set.of("A", "B"));
        assertEquals(80, df.getRowDimension());
        Set<String> tagSet = Set.of("A", "B");
        assertTrue(df.getTags().stream().noneMatch(tagSet::contains));

        df = datasource.get().getDataframeFilteredByNotTags(MODEL_ID, Set.of("A"));
        assertEquals(90, df.getRowDimension());
        tagSet = Set.of("A");
        assertTrue(df.getTags().stream().noneMatch(tagSet::contains));

        // yes tags
        df = datasource.get().getDataframeFilteredByTags(MODEL_ID, Set.of("A", "B", "C"));
        assertEquals(30, df.getRowDimension());
        tagSet = Set.of("A", "B", "C");
        assertTrue(df.getTags().stream().allMatch(tagSet::contains));

        df = datasource.get().getDataframeFilteredByTags(MODEL_ID, Set.of("C"));
        assertEquals(10, df.getRowDimension());
        tagSet = Set.of("C");
        assertTrue(df.getTags().stream().allMatch(tagSet::contains));
    }

}
