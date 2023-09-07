/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.trustyai.explainability.model;

import java.util.Objects;

/**
 * The importance associated to a given {@link Feature}.
 * This is usually the output of an explanation algorithm (local or global).
 */
public class FeatureImportance {

    private final Feature feature;
    private final double score;
    private final double confidence;
    private final double[][] scoreMatrix;

    public FeatureImportance(Feature feature, double score) {
        this.feature = feature;
        this.score = score;
        this.confidence = 0;
        this.scoreMatrix = null;
    }

    public FeatureImportance(Feature feature, double score, double confidence) {
        this.feature = feature;
        this.score = score;
        this.confidence = confidence;
        this.scoreMatrix = null;
    }

    public FeatureImportance(Feature feature, double[][] scoreMatrix, double confidence) {
        this.feature = feature;
        this.score = 0.0;
        this.confidence = confidence;
        this.scoreMatrix = scoreMatrix;
    }

    public Feature getFeature() {
        return feature;
    }

    public double getScore() {
        return score;
    }

    public double getConfidence() {
        return confidence;
    }

    public double[][] getScoreMatrix() {
        return scoreMatrix;
    }

    @Override
    public String toString() {
        return "FeatureImportance{" +
                "feature=" + feature +
                ", score=" + score +
                ", confidence= +/-" + confidence +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FeatureImportance other = (FeatureImportance) o;

        boolean matricesEqual = true;
        if (scoreMatrix != other.scoreMatrix) {
            int T1 = scoreMatrix.length;
            int F1 = scoreMatrix[0].length;

            int T2 = other.scoreMatrix.length;
            int F2 = other.scoreMatrix[0].length;

            if (T1 != T2 || F1 != F2) {
                matricesEqual = false;
            } else {
                for (int t = 0; t < T1; t++) {
                    for (int f = 0; f < F1; f++) {
                        if (scoreMatrix[t][f] - other.scoreMatrix[t][f] > 1e-6) {
                            matricesEqual = false;
                            break;
                        }
                    }

                    if (!matricesEqual) {
                        break;
                    }
                }
            }
        }

        boolean retval;
        if (!matricesEqual) {
            retval = false;
        } else {
            retval = this.getFeature().equals(other.getFeature())
                    && (Math.abs(this.getScore() - other.getScore()) < 1e-6)
                    && (Math.abs(this.getConfidence() - other.getConfidence()) < 1e-6);
        }

        return retval;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feature, score, confidence, scoreMatrix);
    }
}
