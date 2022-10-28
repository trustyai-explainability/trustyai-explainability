/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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

package org.kie.trustyai.explainability.local.shap.background;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.domain.NumericalFeatureDomain;
import org.kie.trustyai.explainability.utils.models.TestModels;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CounterfactualGeneratorTest {
    int N_COUNTERFACTUALS_TO_GENERATE = 10;

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void testDefaultGeneration(int seed) throws ExecutionException, InterruptedException, TimeoutException {
        List<PredictionInput> seeds = new ArrayList<>();
        Random rn = new Random(seed);

        // generate 100 seed prediction inputs, gaussian around {0, 1, 2, 3, 4}
        for (int i = 0; i < 100; i++) {
            List<Feature> features = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                features.add(new Feature(String.valueOf(j),
                        Type.NUMBER,
                        new Value(j + rn.nextGaussian()),
                        false,
                        NumericalFeatureDomain.create(-5, 5)));
            }
            seeds.add(new PredictionInput(features));
        }

        // given some arbitrary linear model
        PredictionProvider model = TestModels.getLinearModel(new double[] { 5., 0., 1., 25., -5. });

        // generate a background such that f(bg) == 0 for all bg in the backgrounds
        PredictionOutput goal = new PredictionOutput(
                List.of(new Output("linear-sum", Type.NUMBER, new Value(0.), 0d)));
        List<PredictionInput> background = CounterfactualGenerator.builder()
                .withModel(model)
                .withTimeoutSeconds(5)
                .withStepCount(10_000L)
                .withGoalThreshold(.01)
                .withRandom(rn)
                .build()
                .generate(seeds, goal, N_COUNTERFACTUALS_TO_GENERATE);
        assertEquals(N_COUNTERFACTUALS_TO_GENERATE, background.size());

        List<PredictionOutput> fnull = model.predictAsync(background).get();
        // make sure the fnull is within the default goal of .01
        for (PredictionOutput output : fnull) {
            assertEquals(0., output.getOutputs().get(0).getValue().asNumber(), .05);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void testChaining(int seed) throws ExecutionException, InterruptedException, TimeoutException {
        List<PredictionInput> seeds = new ArrayList<>();
        Random rn = new Random(seed);

        // generate a single seed point
        List<Feature> features = new ArrayList<>();
        for (int j = 0; j < 5; j++) {
            features.add(new Feature(String.valueOf(j),
                    Type.NUMBER,
                    new Value(0.),
                    false,
                    NumericalFeatureDomain.create(-5, 5)));
        }
        seeds.add(new PredictionInput(features));

        // given some arbitrary linear model
        PredictionProvider model = TestModels.getLinearModel(new double[] { 5., 0., 1., 25., -5. });

        // generate a background such that f(bg) == 0 for all bg in the backgrounds
        List<PredictionOutput> goals = new ArrayList<>();
        for (int i = 0; i < N_COUNTERFACTUALS_TO_GENERATE; i++) {
            goals.add(new PredictionOutput(
                    List.of(new Output("linear-sum", Type.NUMBER, new Value(i / 10.), 0d))));
        }
        List<PredictionInput> background = CounterfactualGenerator.builder()
                .withModel(model)
                .withTimeoutSeconds(5)
                .withStepCount(30_000L)
                .withGoalThreshold(0.01)
                .withRandom(rn)
                .withKSeeds(5)
                .withMaxAttemptCount(10)
                .build()
                .generateRange(seeds, goals, true);
        assertEquals(1, seeds.size());
        assertEquals(N_COUNTERFACTUALS_TO_GENERATE, background.size());
    }
}
