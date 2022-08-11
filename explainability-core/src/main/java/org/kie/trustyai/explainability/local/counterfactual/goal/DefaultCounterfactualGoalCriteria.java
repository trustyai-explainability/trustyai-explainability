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
package org.kie.trustyai.explainability.local.counterfactual.goal;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultCounterfactualGoalCriteria implements CounterfactualGoalCriteria {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultCounterfactualGoalCriteria.class);
    private final double threshold;
    private final List<Output> goals;

    private DefaultCounterfactualGoalCriteria(List<Output> goals, double threshold) {
        this.threshold = threshold;
        this.goals = goals;
    }

    private static final Set<Type> SUPPORTED_CATEGORICAL_TYPES = Set.of(
            Type.CATEGORICAL,
            Type.BOOLEAN,
            Type.TEXT,
            Type.CURRENCY,
            Type.BINARY,
            Type.UNDEFINED);

    @Override
    public GoalScore apply(List<Output> predictions) {
        double distance = 0.0;
        double score = 1.0;
        final int N = predictions.size();

        if (this.goals.size() != predictions.size()) {
            throw new IllegalArgumentException("Prediction size must be equal to goal size");
        }

        for (int i = 0; i < N; i++) {
            final Output prediction = predictions.get(i);
            final Type predictionType = prediction.getType();
            final Output goal = this.goals.get(i);
            final Type goalType = goal.getType();

            score = Math.min(goal.getScore(), score);

            // If the prediction types differ and the prediction is not null, this is not allowed.
            // An allowance is made if the types differ but the prediction is null, since for DMN models
            // there could be a type difference (e.g. a numerical feature is predicted as a textual "null")
            if (predictionType != goalType && goalType != Type.CATEGORICAL) {
                if (Objects.nonNull(prediction.getValue().getUnderlyingObject())) {
                    String message = String.format("Features must have the same type. Feature '%s', has type '%s' and '%s'",
                            prediction.getName(), predictionType.toString(), goalType.toString());
                    logger.error(message);
                    throw new IllegalArgumentException(message);
                } else {
                    distance += GoalScore.getDefaultDistance();
                }
            }

            if (predictionType == Type.NUMBER) {
                final double predictionValue = prediction.getValue().asNumber();
                final double goalValue = goal.getValue().asNumber();
                final double difference = Math.abs(predictionValue - goalValue);
                // If any of the values is zero use the difference instead of change
                // If neither of the values is zero use the change rate
                double temporaryDistance;
                if (Double.isNaN(predictionValue) || Double.isNaN(goalValue)) {
                    String message = String.format("Unsupported NaN or NULL for numeric feature '%s'", prediction.getName());
                    logger.error(message);
                    throw new IllegalArgumentException(message);
                }
                if (predictionValue == 0 || goalValue == 0) {
                    temporaryDistance = difference;
                } else {
                    temporaryDistance = difference / Math.max(predictionValue, goalValue);
                }
                if (temporaryDistance < threshold) {
                    distance += 0.0;
                } else {
                    distance += temporaryDistance;
                }
            } else if (predictionType == Type.DURATION) {
                final Duration predictionValue = (Duration) prediction.getValue().getUnderlyingObject();
                final Duration goalValue = (Duration) goal.getValue().getUnderlyingObject();

                if (Objects.isNull(predictionValue) || Objects.isNull(goalValue)) {
                    distance += GoalScore.getDefaultDistance();
                } else {
                    // Duration distances calculated from value in seconds
                    final double difference = predictionValue.minus(goalValue).abs().getSeconds();
                    // If any of the values is zero use the difference instead of change
                    // If neither of the values is zero use the change rate
                    double temporaryDistance;
                    if (predictionValue.isZero() || goalValue.isZero()) {
                        temporaryDistance = difference;
                    } else {
                        temporaryDistance = difference / Math.max(predictionValue.getSeconds(), goalValue.getSeconds());
                    }
                    if (temporaryDistance < threshold) {
                        distance += 0.0;
                    } else {
                        distance += temporaryDistance;
                    }
                }
            } else if (predictionType == Type.TIME) {
                final LocalTime predictionValue = (LocalTime) prediction.getValue().getUnderlyingObject();
                final LocalTime goalValue = (LocalTime) goal.getValue().getUnderlyingObject();

                if (Objects.isNull(predictionValue) || Objects.isNull(goalValue)) {
                    distance += GoalScore.getDefaultDistance();
                } else {
                    final double interval = LocalTime.MIN.until(LocalTime.MAX, ChronoUnit.SECONDS);
                    // Time distances calculated from value in seconds
                    final double temporaryDistance = Math.abs(predictionValue.until(goalValue, ChronoUnit.SECONDS)) / interval;
                    if (temporaryDistance < threshold) {
                        distance += 0.0;
                    } else {
                        distance += temporaryDistance;
                    }
                }
            } else if (SUPPORTED_CATEGORICAL_TYPES.contains(predictionType)) {
                final Object goalValueObject = goal.getValue().getUnderlyingObject();
                final Object predictionValueObject = prediction.getValue().getUnderlyingObject();
                distance += (Objects.equals(goalValueObject, predictionValueObject) ? 0.0 : GoalScore.getDefaultDistance());
            } else {
                String message =
                        String.format("Feature '%s' has unsupported type '%s'", prediction.getName(),
                                predictionType.toString());
                logger.error(message);
                throw new IllegalArgumentException(message);
            }
        }
        return GoalScore.create(distance, score);
    }

    public static CounterfactualGoalCriteria create(List<Output> goals, double threshold) {
        return new DefaultCounterfactualGoalCriteria(goals, threshold);
    }

    public static CounterfactualGoalCriteria create(List<Output> goals) {
        return new DefaultCounterfactualGoalCriteria(goals, 0.0);
    }
}
