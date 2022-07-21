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

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

import static org.junit.jupiter.api.Assertions.*;

class RandomGeneratorTest {
    @Test
    void testGeneration() {
        List<PredictionInput> seeds = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            seeds.add(new PredictionInput(List.of(new Feature("f1", Type.NUMBER, new Value(i % 2 == 0 ? 1 : 0)))));
        }

        List<PredictionInput> background = new RandomGenerator(seeds).generate(1000);
        double ones = background.stream().mapToDouble(bg -> bg.getFeatures().get(0).getValue().asNumber()).sum();

        // either of these will be false 1 in 7.3 billion times if we are sampling uniformly
        assertTrue(ones > 400);
        assertTrue(ones < 600);
    }
}
