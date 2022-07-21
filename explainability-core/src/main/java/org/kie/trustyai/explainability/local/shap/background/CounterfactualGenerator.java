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
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Pair;
import org.kie.trustyai.explainability.local.counterfactual.CounterfactualConfig;
import org.kie.trustyai.explainability.local.counterfactual.CounterfactualExplainer;
import org.kie.trustyai.explainability.local.counterfactual.CounterfactualResult;
import org.kie.trustyai.explainability.local.counterfactual.SolverConfigBuilder;
import org.kie.trustyai.explainability.local.counterfactual.entities.CounterfactualEntity;
import org.kie.trustyai.explainability.model.CounterfactualPrediction;
import org.kie.trustyai.explainability.model.PerturbationContext;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.utils.DataUtils;
import org.kie.trustyai.explainability.utils.MatrixUtilsExtensions;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

public class CounterfactualGenerator {
    private final List<PredictionInput> seeds;
    private final Integer kSeeds;
    private final PredictionProvider model;
    private final CounterfactualConfig counterfactualConfig;
    private final PredictionOutput goal;
    private final PerturbationContext pc;
    private final long runningSeconds;

    /**
     * Create a Counterfactual Background Generator
     *
     * @param seeds: All or a subset of the available training {@link PredictionInput}s
     * @param kSeeds: The number of seed points to use in the counterfactual generation.
     *        * If {@param kSeeds} = {@param n}, each background point will be generated from a unique seed {@link PredictionInput}
     *        * If {@param kSeeds} = 1, each background point will be generated from the same seed {@link PredictionInput}, whichever is closest to the given {@param goal}
     *        * If {@param kSeeds} = null, will automatically be chosen as min({@param seeds}.sizeb(), {@param n}) during generation
     * @param model: The {@link PredictionProvider} being explained
     * @param goal: The desired fnull (intercept) value of the ShapKernelExplainer, given by a {@link PredictionOutput}
     * @param counterfactualConfig: The {@link CounterfactualConfig} to be used in the counterfactual search
     * @param pc: The {@link PerturbationContext} to be used in the counterfactual search
     * @param runningSeconds: The number of seconds to run the counterfactual search. The timeout is automatically
     *        set to {@param runningSeconds} + 10
     */
    protected CounterfactualGenerator(List<PredictionInput> seeds, Integer kSeeds, PredictionProvider model,
            PredictionOutput goal, CounterfactualConfig counterfactualConfig,
            PerturbationContext pc, long runningSeconds) {
        this.seeds = seeds;
        this.kSeeds = kSeeds;
        this.model = model;
        this.goal = goal;
        this.counterfactualConfig = counterfactualConfig;
        this.pc = pc;
        this.runningSeconds = runningSeconds;
    }

    /**
     * Generate $n counterfactual background points from the provided seed points.
     *
     * @param n: The number of points to generate
     */
    public List<PredictionInput> generate(int n) throws ExecutionException, InterruptedException, TimeoutException {
        // find the starting points for the search
        int kSeeds;
        if (this.kSeeds == null) {
            kSeeds = Math.min(this.seeds.size(), n);
        } else {
            kSeeds = this.kSeeds;
        }
        List<PredictionInput> bestSeeds = findNClosestSeeds(this.model, this.seeds, this.goal, kSeeds);

        // configure Counterfactual search
        final CounterfactualExplainer counterfactualExplainer = new CounterfactualExplainer(this.counterfactualConfig);

        // run search
        List<PredictionInput> generatedBackground = new ArrayList<>();
        PredictionInput seedPerturb;
        while (generatedBackground.size() < n) {
            for (PredictionInput seed : bestSeeds) {
                System.out.println("======================================");
                // if we've used this seed already, perturb it a bit
                if (generatedBackground.size() > bestSeeds.size()) {
                    seedPerturb = new PredictionInput(DataUtils.perturbFeatures(seed.getFeatures(), this.pc));
                } else {
                    seedPerturb = seed;
                }

                // find possible CF
                Prediction prediction = new CounterfactualPrediction(
                        seedPerturb,
                        goal,
                        null,
                        UUID.randomUUID(),
                        this.runningSeconds);
                final CounterfactualResult counterfactualResult = counterfactualExplainer.explainAsync(prediction, model)
                        .get(this.runningSeconds + 10, TimeUnit.SECONDS);

                // add it to our found list if valid
                System.out.println(MatrixUtilsExtensions.vectorFromPredictionInput(seed));
                System.out.println(counterfactualResult.isValid());
                System.out.println(String.valueOf(counterfactualResult.isValid()) + counterfactualResult.getOutput().get(0).getOutputs());
                if (counterfactualResult.isValid()) {
                    generatedBackground.add(
                            new PredictionInput(
                                    counterfactualResult.getEntities().stream()
                                            .map(CounterfactualEntity::asFeature).collect(Collectors.toList())));
                }
            }
        }
        return generatedBackground;
    }

    private static List<PredictionInput> findNClosestSeeds(PredictionProvider model, List<PredictionInput> seeds,
            PredictionOutput goal, int n)
            throws ExecutionException, InterruptedException {
        List<PredictionOutput> seedOutputs = model.predictAsync(seeds).get();
        RealVector goalVector = MatrixUtilsExtensions.vectorFromPredictionOutput(goal);
        List<Pair<Integer, Double>> distances = new ArrayList<>();
        for (int i = 0; i < seedOutputs.size(); i++) {
            distances.add(new Pair<>(i, MatrixUtilsExtensions.vectorFromPredictionOutput(
                    new PredictionOutput(seedOutputs.get(i).getOutputs())).getDistance(goalVector)));
        }
        distances.sort(Comparator.comparingDouble(Pair::getValue));
        return IntStream.range(0, n).mapToObj(i -> seeds.get(distances.get(i).getKey())).collect(Collectors.toList());
    }

    // BUILDER FOR CF GENERATOR ========================================================================================
    public static Builder builder(List<PredictionInput> seeds, PredictionProvider model, PredictionOutput goal) {
        return new Builder(seeds, model, goal);
    }

    public static class Builder {
        private final List<PredictionInput> seeds;
        private Integer kSeeds = null;
        private final PredictionProvider model;
        private CounterfactualConfig counterfactualConfig;
        private final PredictionOutput goal;
        private PerturbationContext pc = new PerturbationContext(new Random(), 0);
        private long runningSeconds = 30;
        private long stepCount = 30_000L;

        private Builder(List<PredictionInput> seeds, PredictionProvider model, PredictionOutput goal) {
            this.seeds = seeds;
            this.model = model;
            this.goal = goal;
        }

        public Builder withKSeeds(int kSeeds) {
            this.kSeeds = kSeeds;
            return this;
        }

        public Builder withCounterfactualConfig(CounterfactualConfig counterfactualConfig) {
            this.counterfactualConfig = counterfactualConfig;
            return this;
        }

        public Builder withPerturbationContext(PerturbationContext pc) {
            this.pc = pc;
            return this;
        }

        public Builder withTimeoutSeconds(long runningSeconds) {
            this.runningSeconds = runningSeconds;
            return this;
        }

        public Builder withStepCount(long stepCount) {
            this.stepCount = stepCount;
            return this;
        }

        public CounterfactualGenerator build() {
            final TerminationConfig terminationConfig =
                    new TerminationConfig().withScoreCalculationCountLimit(this.stepCount);
            final SolverConfig solverConfig = SolverConfigBuilder
                    .builder().withTerminationConfig(terminationConfig).build();
            solverConfig.setRandomSeed(0L);
            solverConfig.setEnvironmentMode(EnvironmentMode.REPRODUCIBLE);
            this.counterfactualConfig = new CounterfactualConfig();
            this.counterfactualConfig.withSolverConfig(solverConfig).withGoalThreshold(.01);
            return new CounterfactualGenerator(
                    seeds, kSeeds, model, goal, counterfactualConfig, pc, runningSeconds);
        }
    }
}
