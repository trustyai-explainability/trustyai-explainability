/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.trustyai.metrics.explainability;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.Config;
import org.kie.trustyai.explainability.TestUtils;
import org.kie.trustyai.explainability.local.LocalExplainer;
import org.kie.trustyai.explainability.local.lime.LimeConfig;
import org.kie.trustyai.explainability.local.lime.LimeExplainer;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.utils.LocalSaliencyStability;
import org.kie.trustyai.explainability.utils.models.TestModels;

import static java.util.Collections.emptyList;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExplainabilityMetricsTest {
    int N_TRIALS = 4;

    @Test
    void testExplainabilityNoExplanation() {
        double v = ExplainabilityMetrics.quantifyExplainability(0, 0, 0);
        assertFalse(Double.isNaN(v));
        assertFalse(Double.isInfinite(v));
        assertEquals(0, v);
    }

    @Test
    void testExplainabilityNoExplanationWithInteraction() {
        double v = ExplainabilityMetrics.quantifyExplainability(0, 0, 1);
        assertFalse(Double.isNaN(v));
        assertFalse(Double.isInfinite(v));
        assertEquals(0, v);
    }

    @Test
    void testExplainabilitySameIOChunksNoInteraction() {
        double v = ExplainabilityMetrics.quantifyExplainability(10, 10, 0);
        assertFalse(Double.isNaN(v));
        assertFalse(Double.isInfinite(v));
        assertThat(v).isBetween(0d, 1d);
    }

    @Test
    void testExplainabilitySameIOChunksWithInteraction() {
        double v = ExplainabilityMetrics.quantifyExplainability(10, 10, 0.5);
        assertEquals(0.2331, v, 1e-5);
    }

    @Test
    void testExplainabilityDifferentIOChunksNoInteraction() {
        double v = ExplainabilityMetrics.quantifyExplainability(3, 9, 0);
        assertEquals(0.481, v, 1e-5);
    }

    @Test
    void testExplainabilityDifferentIOChunksInteraction() {
        double v = ExplainabilityMetrics.quantifyExplainability(3, 9, 0.5);
        assertEquals(0.3145, v, 1e-5);
    }

    @Disabled("Undiagnosed timeouts when run on GitHub")
    @Test
    void testFidelityWithTextClassifier() throws ExecutionException, InterruptedException, TimeoutException {
        List<Pair<Saliency, Prediction>> pairs = new LinkedList<>();
        LimeConfig limeConfig = new LimeConfig()
                .withSamples(10);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        PredictionProvider model = TestModels.getDummyTextClassifier();
        List<Feature> features = new LinkedList<>();
        features.add(FeatureFactory.newFulltextFeature("f-0", "brown fox", s -> Arrays.asList(s.split(" "))));
        features.add(FeatureFactory.newTextFeature("f-1", "money"));
        PredictionInput input = new PredictionInput(features);
        Prediction prediction = new SimplePrediction(
                input,
                model.predictAsync(List.of(input))
                        .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                        .get(0));
        Map<String, Saliency> saliencyMap = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit()).getSaliencies();
        for (Saliency saliency : saliencyMap.values()) {
            pairs.add(Pair.of(saliency, prediction));
        }
        Assertions.assertDoesNotThrow(() -> {
            ExplainabilityMetrics.classificationFidelity(pairs);
        });
    }

    @Test
    void testFidelityWithEvenSumModel() throws ExecutionException, InterruptedException, TimeoutException {
        List<Pair<Saliency, Prediction>> pairs = new LinkedList<>();
        LimeConfig limeConfig = new LimeConfig()
                .withSamples(10);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);

        PredictionProvider model = TestModels.getEvenSumModel(1);
        List<Feature> features = new LinkedList<>();
        features.add(FeatureFactory.newNumericalFeature("f-1", 1));
        features.add(FeatureFactory.newNumericalFeature("f-2", 2));
        features.add(FeatureFactory.newNumericalFeature("f-3", 3));
        PredictionInput input = new PredictionInput(features);
        Prediction prediction = new SimplePrediction(
                input,
                model.predictAsync(List.of(input))
                        .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                        .get(0));
        Map<String, Saliency> saliencyMap = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit()).getSaliencies();
        for (Saliency saliency : saliencyMap.values()) {
            pairs.add(Pair.of(saliency, prediction));
        }
        Assertions.assertDoesNotThrow(() -> {
            ExplainabilityMetrics.classificationFidelity(pairs);
        });
    }

    @Test
    void testBrokenPredict() {
        Config.INSTANCE.setAsyncTimeout(1);
        Config.INSTANCE.setAsyncTimeUnit(TimeUnit.MILLISECONDS);
        Prediction emptyPrediction = new SimplePrediction(new PredictionInput(emptyList()), new PredictionOutput(emptyList()));
        PredictionProvider brokenProvider = inputs -> supplyAsync(
                () -> {
                    await().atLeast(1, TimeUnit.SECONDS).until(() -> false);
                    throw new RuntimeException("this should never happen");
                });

        List<FeatureImportance> emptyFeatures = emptyList();
        try {
            Assertions.assertThrows(IllegalStateException.class,
                    () -> ExplainabilityMetrics.impactScore(brokenProvider, emptyPrediction, emptyFeatures));
        } finally {
            Config.INSTANCE.setAsyncTimeout(Config.DEFAULT_ASYNC_TIMEOUT);
            Config.INSTANCE.setAsyncTimeUnit(Config.DEFAULT_ASYNC_TIMEUNIT);
        }
    }

    @Test
    void testGetLocalSaliencyStability() throws ExecutionException, InterruptedException, TimeoutException {
        PredictionProvider model = TestModels.getSumThresholdDifferentiableModel(0, 0.1);
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            features.add(TestUtils.getMockedNumericFeature(i * 0.1));
        }
        PredictionInput input = new PredictionInput(features);
        Prediction prediction = new SimplePrediction(input, model.predictAsync(List.of(input)).get(Config.DEFAULT_ASYNC_TIMEOUT, Config.DEFAULT_ASYNC_TIMEUNIT).get(0));
        LocalExplainer<SaliencyResults> explainer = new LimeExplainer();
        int topK = 2;
        int runs = N_TRIALS;
        LocalSaliencyStability localSaliencyStability = ExplainabilityMetrics.getLocalSaliencyStability(model, prediction, explainer, topK, runs);
        assertThat(localSaliencyStability).isNotNull();
        assertThat(localSaliencyStability.getDecisions()).isNotNull().isNotEmpty();
        assertThat(localSaliencyStability.getPositiveStabilityScore("distance", 1)).isEqualTo(1);
        assertThat(localSaliencyStability.getNegativeStabilityScore("distance", 1)).isEqualTo(1);
        assertThat(localSaliencyStability.getPositiveStabilityScore("distance", 2)).isEqualTo(1);
        assertThat(localSaliencyStability.getNegativeStabilityScore("distance", 2)).isEqualTo(1);
    }

    @Test
    void testGetLocalSaliencyRecall() throws ExecutionException, InterruptedException, TimeoutException {
        PredictionProvider model = TestModels.getSumThresholdDifferentiableModel(0, 0.1);
        List<Prediction> predictions = new ArrayList<>();
        for (int j = 0; j < N_TRIALS; j++) {
            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                features.add(TestUtils.getMockedNumericFeature(j + i * 0.1));
            }
            PredictionInput input = new PredictionInput(features);
            Prediction prediction = new SimplePrediction(input, model.predictAsync(List.of(input)).get(Config.DEFAULT_ASYNC_TIMEOUT, Config.DEFAULT_ASYNC_TIMEUNIT).get(0));
            predictions.add(prediction);
        }
        LocalExplainer<SaliencyResults> explainer = new LimeExplainer();
        double recall = ExplainabilityMetrics.getLocalSaliencyRecall("inside", model, explainer, predictions, 2, N_TRIALS / 2);
        assertThat(recall).isEqualTo(1);
    }

    @Test
    void testGetLocalSaliencyPrecision() throws ExecutionException, InterruptedException, TimeoutException {
        PredictionProvider model = TestModels.getSumThresholdDifferentiableModel(0, 0.1);
        List<Prediction> predictions = new ArrayList<>();
        for (int j = 0; j < N_TRIALS; j++) {
            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                features.add(TestUtils.getMockedNumericFeature(j + i * 0.1));
            }
            PredictionInput input = new PredictionInput(features);
            Prediction prediction = new SimplePrediction(input, model.predictAsync(List.of(input)).get(Config.DEFAULT_ASYNC_TIMEOUT, Config.DEFAULT_ASYNC_TIMEUNIT).get(0));
            predictions.add(prediction);
        }
        LocalExplainer<SaliencyResults> explainer = new LimeExplainer();
        double recall = ExplainabilityMetrics.getLocalSaliencyPrecision("inside", model, explainer, predictions, 2, N_TRIALS / 2);
        assertThat(recall).isEqualTo(1);
    }

    @Test
    void testGetLocalSaliencyF1() throws ExecutionException, InterruptedException, TimeoutException {
        PredictionProvider model = TestModels.getSumThresholdDifferentiableModel(0, 0.1);
        List<Prediction> predictions = new ArrayList<>();
        for (int j = 0; j < N_TRIALS; j++) {
            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                features.add(TestUtils.getMockedNumericFeature(j + i * 0.1));
            }
            PredictionInput input = new PredictionInput(features);
            Prediction prediction = new SimplePrediction(input, model.predictAsync(List.of(input)).get(Config.DEFAULT_ASYNC_TIMEOUT, Config.DEFAULT_ASYNC_TIMEUNIT).get(0));
            predictions.add(prediction);
        }
        LocalExplainer<SaliencyResults> explainer = new LimeExplainer();
        double recall = ExplainabilityMetrics.getLocalSaliencyF1("inside", model, explainer, predictions, 2, N_TRIALS / 2);
        assertThat(recall).isEqualTo(1);
    }
}
