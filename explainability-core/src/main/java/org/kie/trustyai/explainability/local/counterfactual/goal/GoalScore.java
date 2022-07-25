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

import java.util.Objects;

public class GoalScore {

    private static final double DEFAULT_DISTANCE = 1.0;
    private static GoalScore EXACT_MATCH = GoalScore.create(0.0, 1.0);
    private static GoalScore STANDARD_MISMATCH = GoalScore.create(getDefaultDistance(), 1.0);

    public static double getDefaultDistance() {
        return DEFAULT_DISTANCE;
    }

    public static GoalScore getExactMatch() {
        return EXACT_MATCH;
    }

    public static GoalScore getStandardMismatch() {
        return STANDARD_MISMATCH;
    }

    public Double getDistance() {
        return distance;
    }

    public Double getScore() {
        return score;
    }

    private final Double distance;
    private final Double score;

    private GoalScore(Double distance, Double score) {
        this.distance = distance;
        this.score = score;
    }

    public static GoalScore create(Double distance, Double score) {
        return new GoalScore(distance, score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GoalScore goalScore = (GoalScore) o;
        return distance.equals(goalScore.distance) && score.equals(goalScore.score);
    }

    @Override
    public int hashCode() {
        return Objects.hash(distance, score);
    }
}
