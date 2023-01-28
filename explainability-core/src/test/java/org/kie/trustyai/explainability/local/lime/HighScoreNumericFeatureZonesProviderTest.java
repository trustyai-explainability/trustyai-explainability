/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.utils.models.TestModels;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class HighScoreNumericFeatureZonesProviderTest {

    @Test
    void testEmptyData() {
        List<Feature> features = new ArrayList<>();
        AsyncPredictionProvider predictionProvider = TestModels.getSumThresholdModel(0.1, 0.1);
        List<FeatureDistribution> featureDistributions = new ArrayList<>();
        DataDistribution dataDistribution = new IndependentFeaturesDataDistribution(featureDistributions);
        Map<String, HighScoreNumericFeatureZones> highScoreFeatureZones =
                HighScoreNumericFeatureZonesProvider.getHighScoreFeatureZones(dataDistribution, predictionProvider, features, 10);
        assertThat(highScoreFeatureZones).isNotNull();
        assertThat(highScoreFeatureZones.size()).isZero();
    }

    @Test
    void testNonEmptyData() {
        Random random = new Random();
        random.setSeed(0);
        PerturbationContext perturbationContext = new PerturbationContext(random, 1);
        List<Feature> features = new ArrayList<>();
        AsyncPredictionProvider predictionProvider = TestModels.getSumThresholdModel(0.1, 0.1);
        List<FeatureDistribution> featureDistributions = new ArrayList<>();
        int nf = 4;
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
        Map<String, HighScoreNumericFeatureZones> highScoreFeatureZones =
                HighScoreNumericFeatureZonesProvider.getHighScoreFeatureZones(dataDistribution, predictionProvider, features, 10);
        assertThat(highScoreFeatureZones).isNotNull();
        assertThat(highScoreFeatureZones.size()).isEqualTo(4);
    }

}