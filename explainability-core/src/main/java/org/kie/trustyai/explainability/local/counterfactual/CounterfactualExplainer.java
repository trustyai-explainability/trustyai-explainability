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
package org.kie.trustyai.explainability.local.counterfactual;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.local.LocalExplainer;
import org.kie.trustyai.explainability.local.counterfactual.entities.CounterfactualEntity;
import org.kie.trustyai.explainability.local.counterfactual.entities.CounterfactualEntityFactory;
import org.kie.trustyai.explainability.local.counterfactual.goal.CounterfactualGoalCriteria;
import org.kie.trustyai.explainability.local.counterfactual.score.DefaultCounterfactualScoreCalculator;
import org.kie.trustyai.explainability.model.CounterfactualPrediction;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.utils.CompositeFeatureUtils;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.config.solver.SolverConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides exemplar (counterfactual) explanations for a predictive model.
 * This implementation uses the Constraint Solution Problem solver OptaPlanner to search for
 * counterfactuals which minimize a score calculated by {@link DefaultCounterfactualScoreCalculator}.
 */
public class CounterfactualExplainer implements LocalExplainer<CounterfactualResult> {

    public static final Consumer<CounterfactualSolution> assignSolutionId =
            counterfactual -> counterfactual.setSolutionId(UUID.randomUUID());
    private static final Logger logger =
            LoggerFactory.getLogger(CounterfactualExplainer.class);

    private final CounterfactualConfig counterfactualConfig;

    public CounterfactualExplainer() {
        this.counterfactualConfig = new CounterfactualConfig();
    }

    /**
     * Create a new {@link CounterfactualExplainer} using OptaPlanner as the underlying engine.
     * The data distribution information (if available) will be used to scale the features during the search.
     * A customizable OptaPlanner solver configuration can be passed using a {@link SolverConfig}.
     * An specific {@link Executor} can also be provided.
     * The score calculation (as performed by {@link DefaultCounterfactualScoreCalculator}) will use the goal threshold
     * to if a proposed solution is close enough to the goal to be considered a match. This will only apply
     * to numerical variables. This threshold is a positive ratio of the variable value (e.g. 0.01 of the value)
     * A strict match can be implemented by using a threshold of zero.
     *
     * @param counterfactualConfig An Counterfactual {@link CounterfactualConfig} configuration
     */
    public CounterfactualExplainer(CounterfactualConfig counterfactualConfig) {
        this.counterfactualConfig = counterfactualConfig;
    }

    public CounterfactualConfig getCounterfactualConfig() {
        return this.counterfactualConfig;
    }

    /**
     * Wrap the provided {@link Consumer<CounterfactualResult>} in a OptaPlanner-accepted
     * {@link Consumer<CounterfactualSolution>}.
     * The consumer is only called when the provided {@link CounterfactualSolution} is valid.
     *
     * @param consumer {@link Consumer<CounterfactualResult>} provided to the explainer for intermediate results
     * @return {@link Consumer<CounterfactualSolution>} as accepted by OptaPlanner
     */
    private Consumer<CounterfactualSolution> createSolutionConsumer(Consumer<CounterfactualResult> consumer,
            AtomicLong sequenceId) {
        return counterfactualSolution -> {
            if (counterfactualSolution.getScore().isFeasible()) {
                final List<CounterfactualEntity> entities = counterfactualSolution.getEntities();
                final List<Feature> features = entities.stream().map(CounterfactualEntity::asFeature).collect(Collectors.toList());
                final List<Feature> unflattenedFeatures = CompositeFeatureUtils.unflattenFeatures(features, counterfactualSolution.getOriginalFeatures());
                CounterfactualResult result = new CounterfactualResult(entities, unflattenedFeatures,
                        counterfactualSolution.getPredictionOutputs(),
                        counterfactualSolution.getScore().isFeasible(),
                        counterfactualSolution.getSolutionId(),
                        counterfactualSolution.getExecutionId(),
                        sequenceId.incrementAndGet());
                consumer.accept(result);
            }
        };
    }

    /**
     * Assembles the counterfactual response from the entities returned from the search
     *
     * @param entities
     * @return
     */
    private static List<PredictionInput> buildInput(List<CounterfactualEntity> entities) {
        return List.of(new PredictionInput(
                entities.stream().map(CounterfactualEntity::asFeature).collect(Collectors.toList())));
    }

    private CompletableFuture<CounterfactualResult> search(final List<CounterfactualEntity> entities,
            final List<Feature> originalFeatures,
            final PredictionProvider model,
            final UUID executionId,
            final CounterfactualGoalCriteria goalCriteria,
            final Long maxRunningTimeSeconds,
            final Consumer<CounterfactualResult> intermediateResultsConsumer) {

        logger.debug("executor in use: " + this.counterfactualConfig.getExecutor());

        final AtomicLong sequenceId = new AtomicLong(0);
        Function<UUID, CounterfactualSolution> initial =
                uuid -> new CounterfactualSolution(entities, originalFeatures, model, UUID.randomUUID(), executionId,
                        goalCriteria, this.counterfactualConfig.getGoalThreshold());

        final CompletableFuture<CounterfactualSolution> cfSolution = CompletableFuture.supplyAsync(() -> {
            SolverConfig solverConfig = this.counterfactualConfig.getSolverConfig();
            if (Objects.nonNull(maxRunningTimeSeconds)) {
                solverConfig.withTerminationSpentLimit(Duration.ofSeconds(maxRunningTimeSeconds));
            }
            try (SolverManager<CounterfactualSolution, UUID> solverManager = this.counterfactualConfig
                    .getSolverManagerFactory()
                    .apply(solverConfig)) {

                SolverJob<CounterfactualSolution, UUID> solverJob =
                        solverManager.solveAndListen(executionId, initial,
                                assignSolutionId.andThen(createSolutionConsumer(intermediateResultsConsumer,
                                        sequenceId)),
                                null);
                try {
                    // Wait until the solving ends
                    return solverJob.getFinalBestSolution();
                } catch (ExecutionException e) {
                    logger.error("Solving failed: {}", e.getMessage());
                    throw new IllegalStateException("Prediction returned an error", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Solving failed (Thread interrupted)", e);
                }
            }
        }, this.counterfactualConfig.getExecutor());

        final CompletableFuture<List<PredictionOutput>> cfOutputs =
                cfSolution.thenCompose(s -> model.predictAsync(buildInput(s.getEntities())));
        return CompletableFuture.allOf(cfOutputs, cfSolution).thenApply(v -> {
            CounterfactualSolution solution = cfSolution.join();
            return new CounterfactualResult(solution.getEntities(),
                    solution.getOriginalFeatures(),
                    cfOutputs.join(),
                    solution.getScore().isFeasible(),
                    UUID.randomUUID(),
                    solution.getExecutionId(),
                    sequenceId.incrementAndGet());
        });
    }

    @Override
    public CompletableFuture<CounterfactualResult> explainAsync(Prediction prediction,
            PredictionProvider model,
            Consumer<CounterfactualResult> intermediateResultsConsumer) {

        if (!(prediction instanceof CounterfactualPrediction)) {
            final String message = "Prediction must be an instance of CounterfactualPredicton.";
            logger.error(message);
            throw new IllegalArgumentException(message);
        }

        final CounterfactualPrediction cfPrediction = (CounterfactualPrediction) prediction;

        final List<CounterfactualEntity> entities =
                CounterfactualEntityFactory.createEntities(cfPrediction.getInput());
        final UUID executionId = cfPrediction.getExecutionId();

        // Original features kept as structural reference to re-assemble composite features
        final List<Feature> originalFeatures = cfPrediction.getInput().getFeatures();

        final CounterfactualGoalCriteria goalCriteria = cfPrediction.getGoalCriteria();
        final Long maxRunningTimeSeconds = cfPrediction.getMaxRunningTimeSeconds();

        return search(entities, originalFeatures, model, executionId, goalCriteria, maxRunningTimeSeconds, intermediateResultsConsumer);
    }

}
