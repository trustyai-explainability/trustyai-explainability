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
package org.kie.trustyai.explainability.local.counterfactual.entities;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureDistribution;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.NumericFeatureDistribution;
import org.kie.trustyai.explainability.model.domain.FeatureDomain;
import org.kie.trustyai.explainability.model.domain.NumericalFeatureDomain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DoubleEntityTest {

    @Test
    void distanceUnscaled() {
        final FeatureDomain featureDomain = NumericalFeatureDomain.create(0.0, 40.0);
        final Feature doubleFeature = FeatureFactory.newNumericalFeature("feature-double", 20.0, featureDomain);
        DoubleEntity entity = (DoubleEntity) CounterfactualEntityFactory.from(doubleFeature);
        entity.proposedValue = 30.0;
        assertEquals(10.0, entity.distance());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4 })
    void distanceScaled(int seed) {
        Random random = new Random();
        random.setSeed(seed);

        final FeatureDomain featureDomain = NumericalFeatureDomain.create(0.0, 40.0);
        final Feature doubleFeature = FeatureFactory.newNumericalFeature("feature-double", 20.0, featureDomain);

        final FeatureDistribution featureDistribution = new NumericFeatureDistribution(doubleFeature, random.doubles(5000, 10.0, 40.0).toArray());
        DoubleEntity entity = (DoubleEntity) CounterfactualEntityFactory.from(doubleFeature, featureDistribution);
        entity.proposedValue = 30.0;
        final double distance = entity.distance();
        assertTrue(distance > 0.1 && distance < 0.2);
    }
}