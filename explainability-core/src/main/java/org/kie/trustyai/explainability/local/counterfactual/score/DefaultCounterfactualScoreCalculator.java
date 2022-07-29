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
package org.kie.trustyai.explainability.local.counterfactual.score;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.Config;
import org.kie.trustyai.explainability.local.counterfactual.CounterfactualSolution;
import org.kie.trustyai.explainability.local.counterfactual.entities.CounterfactualEntity;
import org.kie.trustyai.explainability.local.counterfactual.goal.CounterfactualGoalCriteria;
import org.kie.trustyai.explainability.local.counterfactual.goal.GoalScore;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.utils.CompositeFeatureUtils;
import org.optaplanner.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counterfactual score calculator.
 * The score is implementabled as a {@link BendableBigDecimalScore} with two hard levels and one soft level.
 * The primary hard level penalizes solutions which do not meet the required outcome.
 * The second hard level penalizes solutions which change constrained {@link CounterfactualEntity}.
 * The soft level penalizes solutions according to their distance from the original prediction inputs.
 */
public class DefaultCounterfactualScoreCalculator implements CounterfactualScoreCalculator {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultCounterfactualScoreCalculator.class);

    private BendableBigDecimalScore calculateInputScore(CounterfactualSolution solution) {
        int secondarySoftScore = 0;
        int secondaryHardScore = 0;

        // Calculate similarities between original inputs and proposed inputs
        double inputSimilarities = 0.0;
        final int numberOfEntities = solution.getEntities().size();
        for (CounterfactualEntity entity : solution.getEntities()) {
            final double entitySimilarity = entity.similarity();
            inputSimilarities += entitySimilarity / numberOfEntities;

            if (entity.isChanged()) {
                secondarySoftScore -= 1;

                if (entity.isConstrained()) {
                    secondaryHardScore -= 1;
                }
            }
        }

        // Calculate Gower distance from the similarities
        final double primarySoftScore = -Math.sqrt(Math.abs(1.0 - inputSimilarities));

        return BendableBigDecimalScore.of(new BigDecimal[] {
                BigDecimal.ZERO,
                BigDecimal.valueOf(secondaryHardScore),
                BigDecimal.ZERO
        },
                new BigDecimal[] { BigDecimal.valueOf(-Math.abs(primarySoftScore)), BigDecimal.valueOf(secondarySoftScore) });
    }

    private BendableBigDecimalScore calculateOutputScore(CounterfactualSolution solution) {
        final List<PredictionOutput> predictions = solution.getPredictionOutputs();
        final CounterfactualGoalCriteria goalCriteria = solution.getGoalCriteria();

        double outputDistance = 0.0;
        int tertiaryHardScore = 0;
        double primaryHardScore = 0;

        for (PredictionOutput predictionOutput : predictions) {
            final List<Output> outputs = predictionOutput.getOutputs();
            final GoalScore score = goalCriteria.apply(outputs);
            outputDistance += score.getDistance() * score.getDistance();

            for (final Output output : outputs) {
                if (output.getScore() < score.getScore()) {
                    tertiaryHardScore -= 1;
                }
            }
            primaryHardScore -= Math.sqrt(outputDistance);
        }

        return BendableBigDecimalScore.of(
                new BigDecimal[] {
                        BigDecimal.valueOf(primaryHardScore),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(tertiaryHardScore)
                },
                new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO });
    }

    /**
     * Calculates the counterfactual score for each proposed solution.
     * This method assumes that each model used as {@link PredictionProvider} is
     * consistent, in the sense that for repeated operations, the size of the returned collection of
     * {@link PredictionOutput} is the same, if the size of {@link PredictionInput} doesn't change.
     *
     * @param solution Proposed solution
     * @return A {@link BendableBigDecimalScore} with three "hard" levels and one "soft" level
     */
    @Override
    public BendableBigDecimalScore calculateScore(CounterfactualSolution solution) {

        BendableBigDecimalScore currentScore = calculateInputScore(solution);

        final List<Feature> flattenedFeatures =
                solution.getEntities().stream().map(CounterfactualEntity::asFeature).collect(Collectors.toList());

        final List<Feature> input = CompositeFeatureUtils.unflattenFeatures(flattenedFeatures, solution.getOriginalFeatures());

        final List<PredictionInput> inputs = List.of(new PredictionInput(input));

        final CompletableFuture<List<PredictionOutput>> predictionAsync = solution.getModel().predictAsync(inputs);

        try {
            List<PredictionOutput> predictions = predictionAsync.get(Config.INSTANCE.getAsyncTimeout(),
                    Config.INSTANCE.getAsyncTimeUnit());

            solution.setPredictionOutputs(predictions);

            final BendableBigDecimalScore outputScore = calculateOutputScore(solution);

            currentScore = currentScore.add(outputScore);

        } catch (ExecutionException e) {
            logger.error("Prediction returned an error {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Interrupted while waiting for prediction {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (TimeoutException e) {
            logger.error("Timed out while waiting for prediction");
        }
        return currentScore;
    }
}