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
package org.kie.trustyai.explainability.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.TestConfig;
import org.kie.trustyai.explainability.local.lime.LimeConfig;
import org.kie.trustyai.explainability.local.lime.LimeExplainer;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PerturbationContext;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.utils.models.TestModels;

class ValidationUtilsTest {

    @Test
    void testStableEval() throws ExecutionException, InterruptedException, TimeoutException, ValidationUtils.ValidationException {
        for (int n = 0; n < 10; n++) {
            Random random = new Random();
            PerturbationContext perturbationContext = new PerturbationContext(4L, random, 1);
            LimeConfig config = new LimeConfig().withPerturbationContext(perturbationContext);
            LimeExplainer explainer = new LimeExplainer(config);
            PredictionProvider model = TestModels.getSumThresholdModel(0.1, 0.1);
            List<Feature> features = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                features.add(FeatureFactory.newNumericalFeature("f-" + i, Type.NUMBER.randomValue(perturbationContext).asNumber()));
            }
            PredictionInput input = new PredictionInput(features);
            List<PredictionOutput> outputs = model.predictAsync(List.of(input)).get(TestConfig.DEFAULT_ASYNC_TIMEOUT, TestConfig.DEFAULT_ASYNC_TIMEUNIT);
            Prediction prediction = new SimplePrediction(input, outputs.get(0));
            int topK = 1;
            double posScore = 0.6;
            double minScore = 0.6;
            ValidationUtils.validateLocalSaliencyStability(model, prediction, explainer, topK, posScore, minScore);
        }
    }
}
