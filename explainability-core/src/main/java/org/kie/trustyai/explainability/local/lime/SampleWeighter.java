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
package org.kie.trustyai.explainability.local.lime;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.tuple.Pair;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.utils.DataUtils;

/**
 * Utility class to generate weights for the LIME encoded training set, given a prediction.
 */
class SampleWeighter {

    private SampleWeighter() {
        // utility class
    }

    /**
     * Obtain sample weights for a training set, given a list of target input features to compare with.
     * 
     * @param targetInputFeatures target input features
     * @param training the (sparse) training set
     * @param kernelWidth the width of the kernel used to calculate the proximity
     * @return a weight for each sample in the training set
     */
    static double[] getSampleWeightsInterpretable(List<Feature> targetInputFeatures, Collection<Pair<double[], Double>> training,
            double kernelWidth) {
        int noOfFeatures = targetInputFeatures.size();
        double[] x = new double[noOfFeatures];
        Arrays.fill(x, 1);

        return checkNonZero(training.stream().map(Pair::getLeft)
                .map(d -> DataUtils.euclideanDistance(x, d)) // calculate euclidean distance between target and sample points
                .map(d -> DataUtils.exponentialSmoothingKernel(d, kernelWidth)) // transform distance into proximity using an exponential smoothing kernel
                .mapToDouble(Double::doubleValue).toArray()); // output to an array
    }

    /**
     * Obtain sample weights for a training set, given a list of target input features to compare with.
     *
     * @param originalFeatures target input features
     * @param perturbedFeatures the perturbed inputs' features
     * @param kernelWidth the width of the kernel used to calculate the proximity
     * @return a weight for each sample in the training set
     */
    static double[] getSampleWeightsOriginal(List<Feature> originalFeatures, Collection<List<Feature>> perturbedFeatures,
            double kernelWidth) {
        return checkNonZero(perturbedFeatures.stream().mapToDouble(lf -> distance(lf, originalFeatures))
                .map(d -> DataUtils.exponentialSmoothingKernel(d, kernelWidth)).toArray());
    }

    static double distance(List<Feature> f1, List<Feature> f2) {
        assert f1.size() == f2.size() : "cannot calculate the distance between feature sets of different sizes";

        double distance;
        if (f1.stream().allMatch(f -> Type.NUMBER.equals(f.getType())) && f2.stream().allMatch(f -> Type.NUMBER.equals(f.getType()))) {
            distance = DataUtils.euclideanDistance(f1.stream().mapToDouble(f -> f.getValue().asNumber()).toArray(),
                    f2.stream().mapToDouble(f -> f.getValue().asNumber()).toArray());
        } else if (f1.stream().allMatch(f -> Type.TEXT.equals(f.getType())) && f2.stream().allMatch(f -> Type.TEXT.equals(f.getType()))) {
            distance = DataUtils.hammingDistance(f1.stream().map(f -> f.getValue().asString()).collect(Collectors.joining(" ")),
                    f2.stream().map(f -> f.getValue().asString()).collect(Collectors.joining(" ")));
        } else {
            distance = DataUtils.gowerDistance(f1, f2, 0.5);
        }
        return distance;
    }

    static double[] checkNonZero(double[] sampleWeights) {
        if (DoubleStream.of(sampleWeights).allMatch(v -> v == 0)) {
            double[] doubles = new double[sampleWeights.length];
            Arrays.fill(doubles, 1);
            return doubles;
        } else {
            return sampleWeights;
        }
    }
}
