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
import org.kie.trustyai.explainability.TestUtils;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PerturbationContext;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.domain.NumericalFeatureDomain;

import static org.junit.jupiter.api.Assertions.*;

class CounterfactualGeneratorTest {
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
        PredictionProvider model = TestUtils.getLinearModel(new double[] { 5., 0., 1., 25., -5. });

        // generate a background such that f(bg) == 0 for all bg in the backgrounds
        PredictionOutput goal = new PredictionOutput(
                List.of(new Output("linear-sum", Type.NUMBER, new Value(0.), 0d)));
        List<PredictionInput> background = CounterfactualGenerator.builder(seeds, model, goal)
                .withTimeoutSeconds(1)
                .withPerturbationContext(new PerturbationContext(rn, 0))
                .withStepCount(1_000L)
                .withGoalThreshold(.01)
                .build()
                .generate(10);
        List<PredictionOutput> fnull = model.predictAsync(background).get();

        // make sure the fnull is within the default goal of .01
        for (PredictionOutput output : fnull) {
            assertEquals(0., output.getOutputs().get(0).getValue().asNumber(), .05);
        }
    }
}
