package org.kie.trustyai.explainability.model;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.utils.models.TestModels;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

class AsyncPredictionProviderWrapperTest {

    @Test
    void predictAsync() throws ExecutionException, InterruptedException {

        final List<Feature> featureList = List.of(FeatureFactory.newNumericalFeature("f-0", 10.0),
                FeatureFactory.newNumericalFeature("f-1", 20.0), FeatureFactory.newNumericalFeature("f-2", 30.0));
        final List<PredictionInput> inputs = List.of(new PredictionInput(featureList));

        final SyncPredictionProvider modelSync = TestModels.getSumSkipModelSync(1);
        final List<PredictionOutput> outputsSync = modelSync.predictSync(inputs);
        assertEquals(40.0, outputsSync.get(0).getOutputs().get(0).getValue().asNumber());

        final AsyncPredictionProvider modelAsync = TestModels.getSumSkipModel(1);
        final List<PredictionOutput> outputsAsync = modelAsync.predictAsync(inputs).get();
        assertEquals(40.0, outputsAsync.get(0).getOutputs().get(0).getValue().asNumber());

        final AsyncPredictionProvider modelWrap = AsyncPredictionProviderWrapper.from(modelSync);
        final List<PredictionOutput> outputsWrap = modelWrap.predictAsync(inputs).get();
        assertEquals(40.0, outputsWrap.get(0).getOutputs().get(0).getValue().asNumber());
    }

    @Test
    void fromSync() {
        final SyncPredictionProvider modelSync = TestModels.getSumSkipModelSync(1);
        final PredictionProvider modelAsync = AsyncPredictionProviderWrapper.from(modelSync);
        assertTrue(modelAsync instanceof AsyncPredictionProvider);
        assertFalse(modelAsync instanceof SyncPredictionProvider);
    }

    @Test
    void fromAsync() {
        final AsyncPredictionProvider model = TestModels.getSumSkipModel(1);
        final PredictionProvider modelAsync = AsyncPredictionProviderWrapper.from(model);
        assertTrue(modelAsync instanceof AsyncPredictionProvider);
        assertFalse(modelAsync instanceof SyncPredictionProvider);
    }

    @Test
    void fromGeneric() {
        final PredictionProvider model = TestModels.getSumSkipModelSync(1);
        final PredictionProvider modelAsync = AsyncPredictionProviderWrapper.from(model);
        assertTrue(model instanceof SyncPredictionProvider);
        assertFalse(model instanceof AsyncPredictionProvider);
        assertTrue(modelAsync instanceof AsyncPredictionProvider);
        assertFalse(modelAsync instanceof SyncPredictionProvider);
    }
}