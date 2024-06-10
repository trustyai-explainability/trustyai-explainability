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

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.kie.trustyai.explainability.local.counterfactual.goal.CounterfactualGoalCriteria;
import org.kie.trustyai.explainability.model.CounterfactualPrediction;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

public class CounterfactualUtils {

    public static final Long MAX_RUNNING_TIME_SECONDS = 60L;
    public static final long predictionTimeOut = 20L;
    public static final TimeUnit predictionTimeUnit = TimeUnit.MINUTES;
    public static final Long DEFAULT_STEPS = 30_000L;
    public static final double DEFAULT_GOAL_THRESHOLD = 0.01;

    public static CounterfactualResult runCounterfactualSearch(Long randomSeed, List<Output> goal,
            List<Feature> features,
            PredictionProvider model,
            double goalThresold) throws InterruptedException, ExecutionException, TimeoutException {
        return runCounterfactualSearch(randomSeed, goal, features, model, goalThresold, CounterfactualUtils.DEFAULT_STEPS);
    }

    public static CounterfactualResult runCounterfactualSearch(Long randomSeed, List<Output> goal,
            List<Feature> features,
            PredictionProvider model,
            double goalThreshold,
            long steps) throws InterruptedException, ExecutionException, TimeoutException {

        final CounterfactualExplainer explainer = new CounterfactualExplainer(getCounterfactualConfig(randomSeed, steps));
        final PredictionInput input = new PredictionInput(features);
        PredictionOutput output = new PredictionOutput(goal);
        Prediction prediction =
                new CounterfactualPrediction(input,
                        output,
                        goalThreshold,
                        null,
                        UUID.randomUUID(),
                        null);
        return explainer.explainAsync(prediction, model)
                .get(CounterfactualUtils.predictionTimeOut, CounterfactualUtils.predictionTimeUnit);
    }

    public static CounterfactualResult runCounterfactualSearch(Long randomSeed,
            List<Feature> features,
            PredictionProvider model,
            double goalThreshold,
            CounterfactualGoalCriteria goalCriteria,
            long steps) throws InterruptedException, ExecutionException, TimeoutException {

        final CounterfactualExplainer explainer = new CounterfactualExplainer(getCounterfactualConfig(randomSeed, steps));
        final PredictionInput input = new PredictionInput(features);
        Prediction prediction =
                new CounterfactualPrediction(input,
                        null,
                        null,
                        UUID.randomUUID(),
                        null,
                        goalCriteria);
        return explainer.explainAsync(prediction, model)
                .get(CounterfactualUtils.predictionTimeOut, CounterfactualUtils.predictionTimeUnit);
    }

    public static CounterfactualConfig getCounterfactualConfig(Long seed, long steps) {
        Random random = new Random();
        random.setSeed(seed);
        final TerminationConfig terminationConfig = new TerminationConfig().withScoreCalculationCountLimit(steps);
        // for the purpose of this test, only a few steps are necessary
        final SolverConfig solverConfig = SolverConfigBuilder
                .builder().withTerminationConfig(terminationConfig).build();
        solverConfig.setRandomSeed(seed);
        solverConfig.setEnvironmentMode(EnvironmentMode.REPRODUCIBLE);
        return new CounterfactualConfig()
                .withSolverConfig(solverConfig)
                .withExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

}
