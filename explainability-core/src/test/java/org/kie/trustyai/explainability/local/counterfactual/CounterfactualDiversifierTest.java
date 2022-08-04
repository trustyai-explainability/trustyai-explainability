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
package org.kie.trustyai.explainability.local.counterfactual;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.TestUtils;
import org.kie.trustyai.explainability.local.counterfactual.entities.CounterfactualEntity;
import org.kie.trustyai.explainability.model.Dataset;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.domain.NumericalFeatureDomain;
import org.kie.trustyai.explainability.utils.MatrixUtilsExtensions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.kie.trustyai.explainability.local.counterfactual.CounterfactualUtils.DEFAULT_GOAL_THRESHOLD;

class CounterfactualDiversifierTest {

    private static final Logger logger =
            LoggerFactory.getLogger(CounterfactualExplainerTest.class);

    // these tests fail to find diverse counterfactuals; the original CF is too sparse + close to decision
    // boundary for the diversifier to work
    @Disabled
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void simpleDiverse(int seed) throws ExecutionException, InterruptedException, TimeoutException {
        Random random = new Random();
        random.setSeed(seed);

        final List<Output> goal = List.of(
                new Output("inside", Type.BOOLEAN, new Value(true), 0.0d),
                new Output("distance", Type.NUMBER, new Value(0.0), 0.0d));
        List<Feature> features = new LinkedList<>();
        int nfeats = 10;
        for (int i = 0; i < nfeats; i++) {
            features.add(FeatureFactory.newNumericalFeature("f-num" + i, 10.0, NumericalFeatureDomain.create(10.0, 11.0)));
        }
        final double center = nfeats * 11;
        final double epsilon = 5;

        PredictionProvider model = TestUtils.getSumThresholdDifferentiableModel(center, epsilon);
        final CounterfactualResult result =
                CounterfactualUtils.runCounterfactualSearch((long) seed, goal, features, model,
                        DEFAULT_GOAL_THRESHOLD, 100_000L);

        double totalSum = 0;
        System.out.println(MatrixUtilsExtensions.vectorFromPredictionInput(
                new PredictionInput(
                        result.getEntities().stream().map(CounterfactualEntity::asFeature).collect(Collectors.toList()))));
        System.out.println(result.isValid());
        for (CounterfactualEntity entity : result.getEntities()) {
            totalSum += entity.asFeature().getValue().asNumber();
            logger.debug("Entity: {}", entity);
        }
        System.out.println(result.getOutput().get(0).getOutputs());
        logger.debug("Outputs: {}", result.getOutput().get(0).getOutputs());

        assertTrue(totalSum <= center + epsilon);
        assertTrue(totalSum >= center - epsilon);
        assertTrue(result.isValid());

        final CounterfactualDiversifier diversifier = CounterfactualDiversifier.builder(model, features, result, goal).withNSamples(100).build();
        final Dataset diverse = diversifier.diversify(10);
        assertTrue(diverse.getData().size() > 1 && diverse.getData().size() < 10);
        assertTrue(diverse.getOutputs().stream().map(po -> po.getOutputs().get(0).getValue().getUnderlyingObject()).allMatch(x -> x.equals(true)));

        for (Prediction prediction : diverse.getData()) {
            totalSum = prediction.getInput().getFeatures().stream().mapToDouble(f -> f.getValue().asNumber()).sum();
            assertTrue(totalSum <= center + epsilon);
            assertTrue(totalSum >= center - epsilon);
        }
    }

    // these tests fail to find diverse counterfactuals; the original CF is too sparse + close to decision
    // boundary for the diversifier to work
    @Disabled
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void simpleDiverseEvenSumModel(int seed) throws ExecutionException, InterruptedException, TimeoutException {
        Random random = new Random();
        random.setSeed(seed);

        final List<Output> goal = List.of(new Output("sum-even-but1", Type.BOOLEAN, new Value(true), 0.0d));
        List<Feature> features = new LinkedList<>();
        features.add(FeatureFactory.newNumericalFeature("f-num1", 100.0, NumericalFeatureDomain.create(0.0, 1000.0)));
        features.add(FeatureFactory.newNumericalFeature("f-num2", 150.0, NumericalFeatureDomain.create(0.0, 1000.0)));
        features.add(FeatureFactory.newNumericalFeature("f-num3", 1.0, NumericalFeatureDomain.create(0.0, 1000.0)));
        features.add(FeatureFactory.newNumericalFeature("f-num4", 2.0, NumericalFeatureDomain.create(0.0, 1000.0)));

        PredictionProvider model = TestUtils.getEvenSumModel(0);
        final CounterfactualResult result =
                CounterfactualUtils.runCounterfactualSearch((long) seed, goal, features, model,
                        0.1);

        final CounterfactualDiversifier diversifier = CounterfactualDiversifier.builder(model, features, result, goal).build();
        final Dataset diverse = diversifier.diversify(10);
        assertTrue(diverse.getData().size() > 1 && diverse.getData().size() <= 10);
        assertTrue(diverse.getOutputs().stream().map(po -> po.getOutputs().get(0).getValue().getUnderlyingObject()).allMatch(x -> x.equals(true)));
    }

    @Test
    void testGoalMatchTrue() {
        final List<Output> outputs1 = List.of(
                new Output("f-1", Type.NUMBER, new Value(2.3), 1.0),
                new Output("f-2", Type.NUMBER, new Value(5.3), 1.0),
                new Output("f-3", Type.NUMBER, new Value(9.1), 1.0));
        final List<Output> outputs2 = List.of(
                new Output("f-1", Type.NUMBER, new Value(2.3), 1.0),
                new Output("f-2", Type.NUMBER, new Value(5.3), 1.0),
                new Output("f-3", Type.NUMBER, new Value(9.1), 1.0));
        assertTrue(CounterfactualDiversifier.goalMatches(outputs1, outputs2));
    }

    @Test
    void testGoalMatchTrueScore() {
        final List<Output> outputs1 = List.of(
                new Output("f-1", Type.NUMBER, new Value(2.3), 1.0),
                new Output("f-2", Type.NUMBER, new Value(5.3), 1.0),
                new Output("f-3", Type.NUMBER, new Value(9.1), 1.0));
        final List<Output> outputs2 = List.of(
                new Output("f-1", Type.NUMBER, new Value(2.3), 0.1),
                new Output("f-2", Type.NUMBER, new Value(5.3), 0.5),
                new Output("f-3", Type.NUMBER, new Value(9.1), 0.9));
        assertTrue(CounterfactualDiversifier.goalMatches(outputs1, outputs2));
    }

    @Test
    void testGoalMatchFalse() {
        final List<Output> outputs1 = List.of(
                new Output("f-1", Type.NUMBER, new Value(2.3), 1.0),
                new Output("f-2", Type.NUMBER, new Value(5.3), 1.0),
                new Output("f-3", Type.NUMBER, new Value(9.1), 1.0));
        final List<Output> outputs2 = List.of(
                new Output("f-1", Type.NUMBER, new Value(2.3), 0.1),
                new Output("f-2", Type.NUMBER, new Value(5.4), 0.5),
                new Output("f-3", Type.NUMBER, new Value(9.1), 0.9));
        assertFalse(CounterfactualDiversifier.goalMatches(outputs1, outputs2));
    }

}
