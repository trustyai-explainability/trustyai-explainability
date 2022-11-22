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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.TestUtils;
import org.kie.trustyai.explainability.model.Feature;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleWeighterTest {

    @Test
    void testSamplingEmptyDataset() {
        Collection<Pair<double[], Double>> trainingSet = new LinkedList<>();
        List<Feature> features = new LinkedList<>();
        double[] sampleWeights = SampleWeighter.getSampleWeightsInterpretable(features, trainingSet, 0.5);
        assertEquals(0, sampleWeights.length);
    }

    @Test
    void testSamplingInterpretableNonEmptyDataset() {
        Collection<Pair<double[], Double>> trainingSet = new LinkedList<>();
        List<Feature> features = new LinkedList<>();
        for (int i = 0; i < 5; i++) {
            features.add(TestUtils.getMockedNumericFeature(1d));
        }
        // create a dataset whose samples values decrease as the dataset grows (starting from 1)
        for (int i = 0; i < 10; i++) {
            double[] vector = new double[features.size()];
            Arrays.fill(vector, 1d / (1d + i));
            Pair<double[], Double> doubles = Pair.of(vector, 0d);
            trainingSet.add(doubles);
        }
        double[] weights = SampleWeighter.getSampleWeightsInterpretable(features, trainingSet, 0.5);
        assertThat(weights).doesNotContain(0);
        // check that weights decrease with the distance from the 1 vector (the target instance)
        for (int i = 0; i < weights.length - 1; i++) {
            assertTrue(weights[i] > weights[i + 1]);
        }
    }

    @Test
    void testSamplingOriginalNonEmptyDataset() {
        List<Feature> features = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            features.add(TestUtils.getMockedNumericFeature(i));
        }

        Collection<List<Feature>> featureList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<Feature> perturbedFeatures = new ArrayList<>(features);
            perturbedFeatures.set(i % 5, TestUtils.getMockedNumericFeature(i));
            featureList.add(perturbedFeatures);
        }
        double[] weights = SampleWeighter.getSampleWeightsOriginal(features, featureList, 0.5);
        assertThat(weights).doesNotContain(0);
    }
}
