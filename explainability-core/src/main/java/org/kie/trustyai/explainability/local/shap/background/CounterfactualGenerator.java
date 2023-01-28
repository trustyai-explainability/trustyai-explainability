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
import java.util.Optional;
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
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PerturbationContext;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.AsyncPredictionProvider;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.utils.DataUtils;
import org.kie.trustyai.explainability.utils.MatrixUtilsExtensions;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

public class CounterfactualGenerator {
    private final Integer kSeeds;
    private final AsyncPredictionProvider model;
    private final CounterfactualConfig counterfactualConfig;
    private final PerturbationContext pc;
    private final long timeoutSeconds;
    private final double goalThreshold;
    private final int maxAttemptCount;

    /**
     * Create a Counterfactual Background Generator
     *
     * @param kSeeds: The number of seed points to use in the counterfactual generation.
     *        * If {@param kSeeds} = {@param n}, each background point will be generated from a unique seed {@link PredictionInput}
     *        * If {@param kSeeds} = 1, each background point will be generated from the same seed {@link PredictionInput}, whichever is closest to the given {@param goal}
     *        * If {@param kSeeds} = null, will automatically be chosen as min({@param seeds}.sizeb(), {@param n}) during generation
     * @param model: The {@link AsyncPredictionProvider} being explained
     * @param counterfactualConfig: The {@link CounterfactualConfig} to be used in the counterfactual search
     * @param pc: The {@link PerturbationContext} to be used in the counterfactual search
     * @param timeoutSeconds: The number of seconds before the counterfactual search times out.
     */
    protected CounterfactualGenerator(Integer kSeeds, AsyncPredictionProvider model, double goalThreshold, CounterfactualConfig counterfactualConfig,
                                      PerturbationContext pc, long timeoutSeconds, int maxAttemptCount) {
        this.kSeeds = kSeeds;
        this.model = model;
        this.goalThreshold = goalThreshold;
        this.counterfactualConfig = counterfactualConfig;
        this.pc = pc;
        this.timeoutSeconds = timeoutSeconds;
        this.maxAttemptCount = maxAttemptCount;
    }

    // search utils ====================================================================================================
    private static List<PredictionInput> findNClosestSeeds(AsyncPredictionProvider model, List<PredictionInput> seeds,
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
        return IntStream.range(0, Math.min(seeds.size(), n))
                .mapToObj(i -> seeds.get(distances.get(i).getKey())).collect(Collectors.toList());
    }

    /**
     * If we've generated more background than seeds, and already used this seed, perturb it slightly
     */
    private PredictionInput perturbSeed(PredictionInput seed, int nGenerated,
            int bestSeedsSize, int attempts) {
        if (nGenerated > bestSeedsSize || attempts > 0) {

            // take doubles that have whole-number values and _slightly_ perturb them so DataUtils does not think
            // they are integers
            List<Feature> prePerturbFeatures = new ArrayList<>();
            for (Feature f : seed.getFeatures()) {
                if (f.getType() == Type.NUMBER && f.getValue().asNumber() % 1 == 0 &&
                        f.getValue().getUnderlyingObject() instanceof Double) {
                    prePerturbFeatures.add(FeatureFactory.copyOf(f,
                            new Value(f.getValue().asNumber() + Double.MIN_VALUE)));
                }
            }

            return new PredictionInput(DataUtils.perturbFeatures(seed.getFeatures(), this.pc));
        }
        return seed;
    }

    /**
     * Generate $n counterfactual background points from the provided seed points.
     *
     * @param goal: The particular fnull being searched for
     * @param n: The number of points to generate
     */
    public List<PredictionInput> generate(List<PredictionInput> seeds, PredictionOutput goal, int n)
            throws ExecutionException, InterruptedException, TimeoutException {
        // find the starting points for the search
        int kSeeds;
        if (this.kSeeds == null) {
            kSeeds = Math.min(seeds.size(), n);
        } else {
            kSeeds = this.kSeeds;
        }
        List<PredictionInput> bestSeeds = findNClosestSeeds(this.model, seeds, goal, kSeeds);

        // configure Counterfactual search
        final CounterfactualExplainer counterfactualExplainer = new CounterfactualExplainer(this.counterfactualConfig);

        // run search
        List<PredictionInput> generatedBackground = new ArrayList<>();
        int nGenerated = 0;

        PredictionInput seedPerturb;
        int attempts = 0;
        while (nGenerated < n) {
            for (PredictionInput seed : bestSeeds) {
                seedPerturb = this.perturbSeed(seed, nGenerated, bestSeeds.size(), attempts);
                Optional<PredictionInput> generated = singleGenerationAttempt(
                        seedPerturb, goal, counterfactualExplainer);
                if (generated.isPresent()) {
                    generatedBackground.add(generated.get());
                    nGenerated += 1;
                    attempts = 0;
                } else {
                    attempts += 1;
                }
                if (nGenerated <= n) {
                    break;
                }
            }
            if (attempts > this.maxAttemptCount) {
                break;
            }
        }
        return generatedBackground;
    }

    /**
     * Given a seed, goal, and counterfactual explainer, produce a single background point
     */
    private Optional<PredictionInput> singleGenerationAttempt(
            PredictionInput seed, PredictionOutput goal, CounterfactualExplainer counterfactualExplainer)
            throws ExecutionException, InterruptedException, TimeoutException {
        Prediction prediction = new CounterfactualPrediction(
                seed,
                goal,
                this.goalThreshold,
                null,
                UUID.randomUUID(),
                this.timeoutSeconds - 1);
        final CounterfactualResult counterfactualResult = counterfactualExplainer.explainAsync(
                prediction,
                model).get(this.timeoutSeconds, TimeUnit.SECONDS);

        // add it to our found list if valid
        if (counterfactualResult.isValid()) {
            List<CounterfactualEntity> entities = counterfactualResult.getEntities();
            List<Feature> seedFeatures = seed.getFeatures();
            List<Feature> foundFeatures = new ArrayList<>();
            for (int i = 0; i < entities.size(); i++) {
                Feature f = entities.get(i).asFeature();
                foundFeatures.add(new Feature(f.getName(), f.getType(), f.getValue(),
                        seedFeatures.get(i).isConstrained(), seedFeatures.get(i).getDomain()));
            }
            return Optional.of(new PredictionInput(foundFeatures));
        }
        return Optional.empty();
    }

    /**
     * Produce a single background datapoint per goal. IF chaining is true, the found counterfactuals
     * will be added to the counterfactual seeds.
     */
    public List<PredictionInput> generateRange(
            List<PredictionInput> seeds, List<PredictionOutput> goals, int kPerGoal, boolean chain)
            throws ExecutionException, InterruptedException, TimeoutException {
        List<PredictionInput> rangeSeeds = new ArrayList<>(seeds);
        List<PredictionInput> generatedBackground = new ArrayList<>();

        for (int i = 0; i < goals.size(); i++) {
            List<PredictionInput> generated = generate(rangeSeeds, goals.get(i), kPerGoal);
            if (!generated.isEmpty()) {
                if (chain) {
                    rangeSeeds.addAll(generated);
                }
                generatedBackground.addAll(generated);
            }
        }
        return generatedBackground;
    }

    // BUILDER FOR CF GENERATOR ========================================================================================
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer kSeeds = null;
        private AsyncPredictionProvider model;
        private CounterfactualConfig counterfactualConfig;
        private PerturbationContext pc = new PerturbationContext(new Random(), Integer.MAX_VALUE);
        private long timeoutSeconds = 30;
        private long stepCount = 30_000L;
        private double goalThreshold = 0.1;
        private int maxAttemptCount = 5;

        private Builder() {
        }

        public Builder withModel(AsyncPredictionProvider model) {
            this.model = model;
            return this;
        }

        public Builder withKSeeds(int kSeeds) {
            this.kSeeds = kSeeds;
            return this;
        }

        public Builder withCounterfactualConfig(CounterfactualConfig counterfactualConfig) {
            this.counterfactualConfig = counterfactualConfig;
            return this;
        }

        public Builder withRandom(Random rn) {
            this.pc = new PerturbationContext(rn, Integer.MAX_VALUE);
            return this;
        }

        public Builder withTimeoutSeconds(long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder withStepCount(long stepCount) {
            this.stepCount = stepCount;
            return this;
        }

        public Builder withGoalThreshold(double goalThreshold) {
            this.goalThreshold = goalThreshold;
            return this;
        }

        public Builder withMaxAttemptCount(int maxAttemptCount) {
            this.maxAttemptCount = maxAttemptCount;
            return this;
        }

        public CounterfactualGenerator build() {
            final TerminationConfig terminationConfig =
                    new TerminationConfig().withScoreCalculationCountLimit(this.stepCount);
            final SolverConfig solverConfig = SolverConfigBuilder
                    .builder().withTerminationConfig(terminationConfig).build();
            if (this.counterfactualConfig == null) {
                this.counterfactualConfig = new CounterfactualConfig()
                        .withSolverConfig(solverConfig);
            }
            return new CounterfactualGenerator(
                    kSeeds, model, goalThreshold, counterfactualConfig, pc, timeoutSeconds, maxAttemptCount);
        }
    }
}
