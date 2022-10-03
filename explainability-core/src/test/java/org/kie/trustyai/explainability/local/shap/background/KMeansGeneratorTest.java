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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KMeansGeneratorTest {
    @ParameterizedTest
    @ValueSource(ints = { 5, 10, 15 })
    void testKMeansGeneration(int clusters) {
        List<PredictionInput> seeds = new ArrayList<>();

        // deliberately make clustered data with k=5
        List<Double> expectedCentroids = new ArrayList<>();
        for (int j = 0; j < clusters; j++) {
            double expectedCentroid = j * 10;
            expectedCentroids.add(expectedCentroid);
            for (int i = 0; i < 100; i++) {
                seeds.add(new PredictionInput(List.of(new Feature("f1", Type.NUMBER, new Value(j * 10)))));
            }
        }

        // returning 5 clusters should return the exact centroids of the constructed clusters
        List<PredictionInput> background = new KMeansGenerator(seeds, 0).generate(clusters);
        assertTrue(background.stream().allMatch(pi -> expectedCentroids.contains(pi.getFeatures().get(0).getValue().asNumber())));
    }

    @ParameterizedTest
    @ValueSource(ints = { 5, 10, 15 })
    void testKMeansGaussian(int clusters) {
        List<PredictionInput> seeds = new ArrayList<>();
        Random rn = new Random(0L);

        // deliberately make clustered data with k=clusters, each cluster a gaussian distribution around j*100
        List<Double> expectedCentroids = new ArrayList<>();
        for (int j = 0; j < clusters; j++) {
            double expectedCentroid = j * 100;
            expectedCentroids.add(expectedCentroid);
            for (int i = 0; i < 1000; i++) {
                seeds.add(new PredictionInput(List.of(
                        new Feature("f1", Type.NUMBER, new Value(expectedCentroid + rn.nextGaussian())))));
            }
        }

        List<PredictionInput> background = new KMeansGenerator(seeds, 0).generate(clusters);

        // each background point should be close to *one* of the gaussian centroids of the constructed clusters
        assertTrue(background.stream().allMatch(pi -> expectedCentroids.stream().anyMatch(o -> Math.sqrt(Math.pow(o - pi.getFeatures().get(0).getValue().asNumber(), 2)) < .05)));
    }
}
