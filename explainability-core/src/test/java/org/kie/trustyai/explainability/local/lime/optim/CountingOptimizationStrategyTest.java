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
package org.kie.trustyai.explainability.local.lime.optim;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.local.lime.LimeConfig;
import org.kie.trustyai.explainability.local.lime.LimeExplainer;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class CountingOptimizationStrategyTest {

    @Test
    void testNullConfig() {
        LimeOptimizationService optimizationService = mock(LimeOptimizationService.class);
        CountingOptimizationStrategy strategy = new CountingOptimizationStrategy(10, optimizationService);
        assertThat(strategy.bestConfigFor(new LimeExplainer())).isNull();
    }

    @Test
    void testMaybeOptimize() {
        LimeOptimizationService optimizationService = mock(LimeOptimizationService.class);
        CountingOptimizationStrategy strategy = new CountingOptimizationStrategy(10, optimizationService);
        List<Prediction> recordedPredictions = Collections.emptyList();
        PredictionProvider model = mock(PredictionProvider.class);
        LimeExplainer explaier = new LimeExplainer();
        LimeConfig config = new LimeConfig();
        assertThatCode(() -> strategy.maybeOptimize(recordedPredictions, model, explaier, config)).doesNotThrowAnyException();
    }
}