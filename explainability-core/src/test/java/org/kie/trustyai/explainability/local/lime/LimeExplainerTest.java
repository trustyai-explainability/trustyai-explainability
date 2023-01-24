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
package org.kie.trustyai.explainability.local.lime;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.Config;
import org.kie.trustyai.explainability.TestUtils;
import org.kie.trustyai.explainability.local.LocalExplanationException;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.model.domain.CategoricalFeatureDomain;
import org.kie.trustyai.explainability.utils.models.TestModels;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class LimeExplainerTest {

    private static final int DEFAULT_NO_OF_PERTURBATIONS = 1;

    @ParameterizedTest
    @ValueSource(longs = { 0, 1, 2, 3, 4 })
    void testEmptyPrediction(long seed) throws ExecutionException, InterruptedException, TimeoutException {
        Random random = new Random();
        LimeConfig limeConfig = new LimeConfig()
                .withPerturbationContext(new PerturbationContext(seed, random, DEFAULT_NO_OF_PERTURBATIONS))
                .withSamples(10);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        PredictionInput input = new PredictionInput(Collections.emptyList());
        AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
        PredictionOutput output = model.predictAsync(List.of(input))
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                .get(0);
        Prediction prediction = new SimplePrediction(input, output);

        assertThrows(LocalExplanationException.class, () -> limeExplainer.explainAsync(prediction, model));
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, 1, 2, 3, 4 })
    void testNonEmptyInput(long seed) throws ExecutionException, InterruptedException, TimeoutException {
        Random random = new Random();
        LimeConfig limeConfig = new LimeConfig()
                .withPerturbationContext(new PerturbationContext(seed, random, DEFAULT_NO_OF_PERTURBATIONS))
                .withSamples(10);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            features.add(TestUtils.getMockedNumericFeature(i));
        }
        PredictionInput input = new PredictionInput(features);
        AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
        PredictionOutput output = model.predictAsync(List.of(input))
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                .get(0);
        Prediction prediction = new SimplePrediction(input, output);
        SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
        assertNotNull(saliencyResults);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, 1, 2, 3, 4 })
    void testSparseBalance(long seed) throws InterruptedException, ExecutionException, TimeoutException {
        for (int nf = 1; nf < 4; nf++) {
            Random random = new Random();
            int noOfSamples = 100;
            LimeConfig limeConfigNoPenalty = new LimeConfig()
                    .withPerturbationContext(new PerturbationContext(seed, random, DEFAULT_NO_OF_PERTURBATIONS))
                    .withSamples(noOfSamples)
                    .withPenalizeBalanceSparse(false);
            LimeExplainer limeExplainerNoPenalty = new LimeExplainer(limeConfigNoPenalty);

            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < nf; i++) {
                features.add(TestUtils.getMockedNumericFeature(i));
            }
            PredictionInput input = new PredictionInput(features);
            AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
            PredictionOutput output = model.predictAsync(List.of(input))
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                    .get(0);
            Prediction prediction = new SimplePrediction(input, output);

            SaliencyResults saliencyResultsNoPenalty = limeExplainerNoPenalty.explainAsync(prediction, model)
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
            assertThat(saliencyResultsNoPenalty).isNotNull();

            String decisionName = "sum-but0";
            Saliency saliencyNoPenalty = saliencyResultsNoPenalty.getSaliencies().get(decisionName);

            LimeConfig limeConfig = new LimeConfig()
                    .withPerturbationContext(new PerturbationContext(seed, random, DEFAULT_NO_OF_PERTURBATIONS))
                    .withSamples(noOfSamples)
                    .withPenalizeBalanceSparse(true);
            LimeExplainer limeExplainer = new LimeExplainer(limeConfig);

            SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
            assertThat(saliencyResults).isNotNull();

            Saliency saliency = saliencyResults.getSaliencies().get(decisionName);

            for (int i = 0; i < features.size(); i++) {
                double score = saliency.getPerFeatureImportance().get(i).getScore();
                double scoreNoPenalty = saliencyNoPenalty.getPerFeatureImportance().get(i).getScore();
                assertThat(Math.abs(score)).isLessThanOrEqualTo(Math.abs(scoreNoPenalty));
            }
        }
    }

    @Test
    void testNormalizedWeights() throws InterruptedException, ExecutionException, TimeoutException {
        Random random = new Random();
        LimeConfig limeConfig = new LimeConfig()
                .withNormalizeWeights(true)
                .withPerturbationContext(new PerturbationContext(4L, random, 2))
                .withSamples(10);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        int nf = 4;
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < nf; i++) {
            features.add(TestUtils.getMockedNumericFeature(i));
        }
        PredictionInput input = new PredictionInput(features);
        AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
        PredictionOutput output = model.predictAsync(List.of(input))
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                .get(0);
        Prediction prediction = new SimplePrediction(input, output);

        SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
        assertThat(saliencyResults).isNotNull();

        String decisionName = "sum-but0";
        Saliency saliency = saliencyResults.getSaliencies().get(decisionName);
        List<FeatureImportance> perFeatureImportance = saliency.getPerFeatureImportance();
        for (FeatureImportance featureImportance : perFeatureImportance) {
            assertThat(featureImportance.getScore()).isBetween(0d, 1d);
        }
    }

    @Test
    void testWithDataDistribution() throws InterruptedException, ExecutionException, TimeoutException {
        Random random = new Random();
        PerturbationContext perturbationContext = new PerturbationContext(4L, random, 1);
        List<FeatureDistribution> featureDistributions = new ArrayList<>();

        int nf = 4;
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < nf; i++) {
            Feature numericalFeature = FeatureFactory.newNumericalFeature("f-" + i, Double.NaN);
            features.add(numericalFeature);
            List<Value> values = new ArrayList<>();
            for (int r = 0; r < 4; r++) {
                values.add(Type.NUMBER.randomValue(perturbationContext));
            }
            featureDistributions.add(new GenericFeatureDistribution(numericalFeature, values));
        }

        DataDistribution dataDistribution = new IndependentFeaturesDataDistribution(featureDistributions);
        LimeConfig limeConfig = new LimeConfig()
                .withDataDistribution(dataDistribution)
                .withPerturbationContext(perturbationContext)
                .withSamples(10);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        PredictionInput input = new PredictionInput(features);
        AsyncPredictionProvider model = TestModels.getSumThresholdModel(random.nextDouble(), random.nextDouble());
        PredictionOutput output = model.predictAsync(List.of(input))
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                .get(0);
        Prediction prediction = new SimplePrediction(input, output);

        SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
        assertThat(saliencyResults).isNotNull();

        String decisionName = "inside";
        Saliency saliency = saliencyResults.getSaliencies().get(decisionName);
        assertThat(saliency).isNotNull();
    }

    @Test
    void testZeroSampleSize() throws ExecutionException, InterruptedException, TimeoutException {
        LimeConfig limeConfig = new LimeConfig()
                .withSamples(0);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            features.add(TestUtils.getMockedNumericFeature(i));
        }
        PredictionInput input = new PredictionInput(features);
        AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
        PredictionOutput output = model.predictAsync(List.of(input))
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                .get(0);
        Prediction prediction = new SimplePrediction(input, output);
        SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
        assertNotNull(saliencyResults);
    }

    @ParameterizedTest
    @ValueSource(longs = { 0, 1, 2, 3, 4 })
    void testDeterministic(long seed) throws ExecutionException, InterruptedException, TimeoutException {
        List<Saliency> saliencies = new ArrayList<>();
        for (int j = 0; j < 2; j++) {
            Random random = new Random();
            LimeConfig limeConfig = new LimeConfig()
                    .withPerturbationContext(new PerturbationContext(seed, random, DEFAULT_NO_OF_PERTURBATIONS))
                    .withSamples(10);
            LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                features.add(TestUtils.getMockedNumericFeature(i));
            }
            PredictionInput input = new PredictionInput(features);
            AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
            PredictionOutput output = model.predictAsync(List.of(input))
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                    .get(0);
            Prediction prediction = new SimplePrediction(input, output);
            SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
            saliencies.add(saliencyResults.getSaliencies().get("sum-but0"));
        }
        assertThat(saliencies.get(0).getPerFeatureImportance().stream().map(FeatureImportance::getScore)
                .collect(Collectors.toList()))
                        .isEqualTo(saliencies.get(1).getPerFeatureImportance().stream().map(FeatureImportance::getScore)
                                .collect(Collectors.toList()));
    }

    @Test
    void testEmptyInput() {
        LimeExplainer recordingLimeExplainer = new LimeExplainer();
        AsyncPredictionProvider model = mock(AsyncPredictionProvider.class);
        Prediction prediction = mock(Prediction.class);
        assertThatCode(() -> recordingLimeExplainer.explainAsync(prediction, model)).hasMessage("cannot explain a prediction whose input is empty");
    }

    @Test
    void testFeatureSelectionHighestWeights() throws ExecutionException, InterruptedException, TimeoutException {
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            features.add(TestUtils.getMockedNumericFeature(i));
        }
        PredictionInput input = new PredictionInput(features);
        AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
        PredictionOutput output = model.predictAsync(List.of(input))
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                .get(0);
        Prediction prediction = new SimplePrediction(input, output);

        Random random = new Random();
        LimeConfig limeConfig = new LimeConfig()
                .withPerturbationContext(new PerturbationContext(0L, random, DEFAULT_NO_OF_PERTURBATIONS))
                .withSamples(10)
                .withFeatureSelection(false);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
        assertNotNull(saliencyResults);
        List<FeatureImportance> perFeatureImportance = saliencyResults.getSaliencies().get("sum-but0").getPerFeatureImportance();
        assertThat(perFeatureImportance.size()).isEqualTo(10);

        limeExplainer = new LimeExplainer(limeConfig.withFeatureSelection(true));
        saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
        assertNotNull(saliencyResults);
        List<FeatureImportance> filteredFeatureImportance = saliencyResults.getSaliencies().get("sum-but0").getPerFeatureImportance();
        assertThat(filteredFeatureImportance.size()).isEqualTo(6);
    }

    @Test
    void testFeatureSelectionForwardSelection() throws ExecutionException, InterruptedException, TimeoutException {
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            features.add(TestUtils.getMockedNumericFeature(i));
        }
        PredictionInput input = new PredictionInput(features);
        AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
        PredictionOutput output = model.predictAsync(List.of(input))
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                .get(0);
        Prediction prediction = new SimplePrediction(input, output);

        Random random = new Random();
        LimeConfig limeConfig = new LimeConfig()
                .withPerturbationContext(new PerturbationContext(0L, random, DEFAULT_NO_OF_PERTURBATIONS))
                .withSamples(10)
                .withFeatureSelection(false);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
        assertNotNull(saliencyResults);
        List<FeatureImportance> perFeatureImportance = saliencyResults.getSaliencies().get("sum-but0").getPerFeatureImportance();
        assertThat(perFeatureImportance.size()).isEqualTo(5);

        limeExplainer = new LimeExplainer(limeConfig.withFeatureSelection(true).withNoOfFeatures(3));
        saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(111111, Config.INSTANCE.getAsyncTimeUnit());
        assertNotNull(saliencyResults);
        List<FeatureImportance> filteredFeatureImportance = saliencyResults.getSaliencies().get("sum-but0").getPerFeatureImportance();
        assertThat(filteredFeatureImportance.size()).isEqualTo(3);
    }

    @Test
    void testToString() throws ExecutionException, InterruptedException, TimeoutException {
        Random random = new Random(0L);
        LimeConfig limeConfig = new LimeConfig()
                .withPerturbationContext(new PerturbationContext(0L, random, DEFAULT_NO_OF_PERTURBATIONS))
                .withSamples(10);
        LimeExplainer limeExplainer = new LimeExplainer(limeConfig);
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            if (i == 2) {
                features.add(new Feature(
                        "Feature " + i,
                        Type.CATEGORICAL,
                        new Value("A"),
                        false,
                        CategoricalFeatureDomain.create(List.of("A", "B"))));
            } else {
                features.add(new Feature("Feature " + i, Type.NUMBER, new Value(i)));
            }
        }
        PredictionInput input = new PredictionInput(features);
        AsyncPredictionProvider model = TestModels.getTwoOutputSemiCategoricalModel(2);
        PredictionOutput output = model.predictAsync(List.of(input))
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                .get(0);
        Prediction prediction = new SimplePrediction(input, output);
        SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());
        String table = saliencyResults.asTable();
        assertTrue(table.contains("Prediction"));
        assertTrue(table.contains("=== Semi-Categorical LIME Saliencies ===="));
        assertTrue(table.contains("Feature 2 =           A "));
    }

    @ParameterizedTest
    @ValueSource(ints = { 100, 200, 33 })
    void testByproductCounterfactuals(int noOfSamples) throws InterruptedException, ExecutionException, TimeoutException {
        for (int nf = 1; nf < 4; nf++) {
            Long seed = 0L;
            Random random = new Random();
            LimeConfig limeConfig = new LimeConfig()
                    .withPerturbationContext(new PerturbationContext(seed, random, DEFAULT_NO_OF_PERTURBATIONS))
                    .withSamples(noOfSamples)
                    .withAdaptiveVariance(false)
                    .withTrackCounterfactuals(true);
            LimeExplainer limeExplainer = new LimeExplainer(limeConfig);

            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < nf; i++) {
                features.add(TestUtils.getMockedNumericFeature(i));
            }
            PredictionInput input = new PredictionInput(features);
            AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
            PredictionOutput output = model.predictAsync(List.of(input))
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                    .get(0);
            Prediction prediction = new SimplePrediction(input, output);
            SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());

            // check that LIME returns the expected number of CFs
            assertEquals(noOfSamples, saliencyResults.getAvailableCFs().size());
        }
    }

    @Test
    void testNoByproductCounterfactuals() throws InterruptedException, ExecutionException, TimeoutException {
        for (int nf = 1; nf < 4; nf++) {
            Long seed = 0L;
            Random random = new Random();
            int noOfSamples = 100;
            LimeConfig limeConfig = new LimeConfig()
                    .withPerturbationContext(new PerturbationContext(seed, random, DEFAULT_NO_OF_PERTURBATIONS))
                    .withSamples(noOfSamples)
                    .withAdaptiveVariance(false)
                    .withTrackCounterfactuals(false);
            LimeExplainer limeExplainer = new LimeExplainer(limeConfig);

            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < nf; i++) {
                features.add(TestUtils.getMockedNumericFeature(i));
            }
            PredictionInput input = new PredictionInput(features);
            AsyncPredictionProvider model = TestModels.getSumSkipModel(0);
            PredictionOutput output = model.predictAsync(List.of(input))
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit())
                    .get(0);
            Prediction prediction = new SimplePrediction(input, output);
            SaliencyResults saliencyResults = limeExplainer.explainAsync(prediction, model)
                    .get(Config.INSTANCE.getAsyncTimeout(), Config.INSTANCE.getAsyncTimeUnit());

            // check that LIME returns thhe expected number of CFs
            assertEquals(0, saliencyResults.getAvailableCFs().size());
        }
    }
}
