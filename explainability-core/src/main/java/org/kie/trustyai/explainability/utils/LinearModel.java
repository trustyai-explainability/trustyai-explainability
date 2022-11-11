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
package org.kie.trustyai.explainability.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A linear model implementation based on perceptron algorithm.
 */
public class LinearModel {

    private static final Logger logger = LoggerFactory.getLogger(LinearModel.class);
    private static final double GOOD_LOSS_THRESHOLD = 1e-2;
    private static final int MAX_NO_EPOCHS = 5000;
    private static final double INITIAL_LEARNING_RATE = 1e-2;
    private static final double DECAY_RATE = 1e-5;

    private static final int PATIENCE = 50;

    private final double[] weights;
    private final boolean classification;
    private double bias;

    public LinearModel(int size, boolean classification, Random random) {
        this.bias = 0;
        this.classification = classification;
        this.weights = new double[size];
        for (int i = 0; i < size; i++) {
            this.weights[i] = random.nextGaussian();
        }
    }

    public double fit(Collection<Pair<double[], Double>> trainingSet) {
        double[] sampleWeights = new double[trainingSet.size()];
        Arrays.fill(sampleWeights, 1);
        return fit(trainingSet, sampleWeights);
    }

    public double fitWLRR(Collection<Pair<double[], Double>> trainingSet, double[] sampleWeights) {

        Optional<Pair<double[], Double>> first = trainingSet.stream().findFirst();
        if (first.isPresent()) {
            double[][] x = new double[trainingSet.size()][first.get().getLeft().length];
            double[] y = new double[trainingSet.size()];
            int idx = 0;
            for (Pair<double[], Double> datapoint : trainingSet) {
                x[idx] = datapoint.getLeft();
                y[idx] = datapoint.getRight();
                idx++;
            }

            RealMatrix xMat = MatrixUtils.createRealMatrix(x);
            RealVector yVect = MatrixUtils.createRealVector(y);
            RealVector sampleVect = MatrixUtils.createRealVector(sampleWeights).getSubVector(0, yVect.getDimension());

            WeightedLinearRegressionResults wlrr = WeightedLinearRegression.fit(xMat, yVect, sampleVect, false, false);
            System.arraycopy(wlrr.getCoefficients().toArray(), 0, weights, 0, weights.length);
            return wlrr.getMSE();
        } else {
            logger.warn("fitting an empty training set");
            Arrays.fill(weights, 0);
            return Double.NaN;
        }
    }

    public double fit(Collection<Pair<double[], Double>> trainingSet, double[] sampleWeights) {
        double finalLoss = Double.NaN;
        if (trainingSet.isEmpty()) {
            logger.warn("fitting an empty training set");
            Arrays.fill(weights, 0);
            return finalLoss;
        }
        double bestLoss = Double.MAX_VALUE;
        double[] bestWeights = new double[weights.length];
        double bestBias = bias;
        double lr = INITIAL_LEARNING_RATE;
        int e = 0;
        int lossNotDecreasingIterations = 0;

        while (checkFinalLoss(finalLoss) && e < MAX_NO_EPOCHS) {
            double loss = 0; // MAE
            int i = 0;
            for (Pair<double[], Double> sample : trainingSet) {
                double[] doubles = sample.getLeft();
                double predictedOutput = predict(doubles);
                double targetOutput = sample.getRight();
                double diff = finiteOrZero(targetOutput - predictedOutput);
                if (diff != 0) { // avoid null updates to save computation
                    loss += Math.abs(diff) / trainingSet.size();
                    for (int j = 0; j < weights.length; j++) {
                        double v = lr * diff * doubles[j];
                        if (trainingSet.size() == sampleWeights.length) {
                            v *= sampleWeights[i];
                        }
                        v = finiteOrZero(v);
                        weights[j] += v;
                    }
                    double biasUpdate = lr * diff;
                    if (trainingSet.size() == sampleWeights.length) {
                        biasUpdate *= sampleWeights[i];
                    }
                    bias += biasUpdate;
                }
                i++;
            }
            lr *= (1d / (1d + DECAY_RATE * e)); // learning rate decay
            finalLoss = loss;
            if (finalLoss < bestLoss) {
                bestLoss = finalLoss;
                System.arraycopy(weights, 0, bestWeights, 0, weights.length);
                bestBias = bias;
                logger.debug("weights updated, loss: {}", bestLoss);
                lossNotDecreasingIterations = 0;
            } else {
                lossNotDecreasingIterations++;
            }
            e++;

            logger.debug("epoch {}, loss: {}", e, loss);
            if (lossNotDecreasingIterations > PATIENCE) {
                logger.debug("early stopping at iteration {}", e);
                break;
            }
        }
        bias = bestBias;
        System.arraycopy(bestWeights, 0, weights, 0, weights.length);
        return finalLoss;
    }

    private boolean checkFinalLoss(double finalLoss) {
        return (Double.isNaN(finalLoss) || finalLoss > GOOD_LOSS_THRESHOLD);
    }

    private double finiteOrZero(double diff) {
        if (Double.isNaN(diff) || Double.isInfinite(diff)) {
            diff = 0;
        }
        return diff;
    }

    private double predict(double[] input) {
        double linearCombination = bias + IntStream.range(0, input.length).mapToDouble(i -> input[i] * weights[i]).sum();
        if (classification) {
            linearCombination = linearCombination >= 0 ? 1 : 0;
        }
        return linearCombination;
    }

    public double[] getWeights() {
        return weights;
    }
}
