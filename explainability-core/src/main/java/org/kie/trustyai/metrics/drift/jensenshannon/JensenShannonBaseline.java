package org.kie.trustyai.metrics.drift.jensenshannon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.tensor.Tensor;

/**
 * Cross-validation functions to determine a sensible baseline threshold for Jensen-Shannon
 * Adapted from @christinaexyou's original algorithm
 */
public class JensenShannonBaseline {
    private double minThreshold;
    private double maxThreshold;
    private double avgThreshold;

    public JensenShannonBaseline(double minThreshold, double maxThreshold, double avgThreshold) {
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.avgThreshold = avgThreshold;
    }

    public double getMinThreshold() {
        return minThreshold;
    }

    public void setMinThreshold(double minThreshold) {
        this.minThreshold = minThreshold;
    }

    public double getMaxThreshold() {
        return maxThreshold;
    }

    public void setMaxThreshold(double maxThreshold) {
        this.maxThreshold = maxThreshold;
    }

    public double getAvgThreshold() {
        return avgThreshold;
    }

    public void setAvgThreshold(double avgThreshold) {
        this.avgThreshold = avgThreshold;
    }

    @Override
    public String toString() {
        return "JensenShannonBaseline{" +
                "minThreshold=" + minThreshold +
                ", maxThreshold=" + maxThreshold +
                ", avgThreshold=" + avgThreshold +
                '}';
    }

    // === CALCULATORS =================================================================================================
    /**
     * Calculates baseline JS divergence thresholds for reference tensors through cross validation
     *
     * @param references The reference tensor set
     * @param numCV The number of cross-validations to run
     * @param cvSize The size of each individual cross-validation sample: two randomly selected, non-overlapping samples of size=cvSize will be compared from the reference set
     * @param randomNumberGenerator: the random number generator to use when selecting random samples. Use this argument to make the baseline cross-validation deterministic.
     *
     * @return a JensenShannonBaseline containing the min, mean, and max JS divergence over all cross-validations
     */
    public static JensenShannonBaseline calculate(Tensor<Double> references, int numCV, int cvSize, Random randomNumberGenerator) {
        return calculate(references, numCV, cvSize, randomNumberGenerator, true);
    }

    /**
     * Calculates baseline JS divergence thresholds for reference tensors through cross validation
     *
     * @param references The reference tensor set
     * @param numCV The number of cross-validations to run
     * @param cvSize The size of each individual cross-validation sample: two randomly selected, non-overlapping samples of size=cvSize will be compared from the reference set
     * @param randomNumberGenerator: the random number generator to use when selecting random samples. Use this argument to make the baseline cross-validation deterministic.
     * @param normalize: whether to normalize the Jensen-Shannon values by tensor size for each cross-validation: this should keep JS baseline results consistent regardless of the number of references
     *        or cvSize
     *
     * @return a JensenShannonBaseline containing the min, mean, and max JS divergence over all cross-validations
     */
    public static JensenShannonBaseline calculate(Tensor<Double> references, int numCV, int cvSize, Random randomNumberGenerator, boolean normalize) {
        double minThreshold = Double.MAX_VALUE;
        double avgThreshold = 0;
        double maxThreshold = -Double.MAX_VALUE;

        if (cvSize * 2 > references.getDimensions(0)) {
            throw new IllegalArgumentException(String.format(
                    "cvSize*2 cannot be larger than the total number of references: cvSize*2=%d, but there are only %d references.",
                    cvSize * 2, references.getDimensions(0)));
        }

        List<Integer> idxs = new ArrayList<>(IntStream.range(0, references.getDimensions(0)).boxed().toList());

        // divide into CV sets
        for (int i = 0; i < numCV; i++) {
            Collections.shuffle(idxs, randomNumberGenerator);
            Tensor<Double> slice1 = references.get(idxs.subList(0, cvSize));
            Tensor<Double> slice2 = references.get(idxs.subList(cvSize, cvSize * 2));
            JensenShannonDriftResult jsdr = JensenShannon.calculate(slice1, slice2, .5, normalize);
            double jsStat = jsdr.getjsStat();
            if (jsStat < minThreshold) {
                minThreshold = jsStat;
            } else if (jsStat > maxThreshold) {
                maxThreshold = jsStat;
            }
            avgThreshold += jsStat;
        }

        return new JensenShannonBaseline(minThreshold, maxThreshold, avgThreshold / numCV);
    }

    // === PER CHANNEL =================================================================================================
    /**
     * Calculates baseline, per-channel JS divergence thresholds for reference tensors through cross validation
     *
     * @param references The reference tensor set
     * @param numCV The number of cross-validations to run
     * @param cvSize The size of each individual cross-validation sample: two randomly selected, non-overlapping samples of size=cvSize will be compared from the reference set
     * @param randomNumberGenerator: the random number generator to use when selecting random samples. Use this argument to make the baseline cross-validation deterministic.
     *
     * @return an array of JensenShannonBaselines, where the ith value containing the min, mean, and max JS divergence of the ith channel over all cross-validations
     */
    public static JensenShannonBaseline[] calculatePerChannel(Tensor<Double> references, int numCV, int cvSize, Random randomNumberGenerator) {
        return calculatePerChannel(references, numCV, cvSize, randomNumberGenerator, true);
    }

    /**
     * Calculates baseline, per-channel JS divergence thresholds for reference tensors through cross validation
     *
     * @param references The reference tensor set
     * @param numCV The number of cross-validations to run
     * @param cvSize The size of each individual cross-validation sample: two randomly selected, non-overlapping samples of size=cvSize will be compared from the reference set
     * @param randomNumberGenerator: the random number generator to use when selecting random samples. Use this argument to make the baseline cross-validation deterministic.
     * @param normalize: whether to normalize the Jensen-Shannon values by tensor size for each cross-validation: this should keep JS baseline results consistent regardless of the number of references
     *        or cvSize
     *
     * @return an array of JensenShannonBaselines, where the ith value containing the min, mean, and max JS divergence of the ith channel over all cross-validations
     */
    public static JensenShannonBaseline[] calculatePerChannel(Tensor<Double> references, int numCV, int cvSize, Random randomNumberGenerator, boolean normalize) {
        int nChannels = references.getDimensions(1);
        double[] minThreshold = new double[nChannels];
        double[] avgThreshold = new double[nChannels];
        double[] maxThreshold = new double[nChannels];
        double[] dummyThresholds = new double[nChannels];
        Arrays.fill(minThreshold, Double.MAX_VALUE);
        Arrays.fill(maxThreshold, -Double.MAX_VALUE);

        if (cvSize * 2 > references.getDimensions(0)) {
            throw new IllegalArgumentException(String.format(
                    "cvSize*2 cannot be larger than the total number of references: cvSize*2=%d, but there are only %d references.",
                    cvSize * 2, references.getDimensions(0)));
        }

        List<Integer> idxs = new ArrayList<>(IntStream.range(0, references.getDimensions(0)).boxed().toList());

        // divide into CV sets
        for (int i = 0; i < numCV; i++) {
            Collections.shuffle(idxs, randomNumberGenerator);
            Tensor<Double> slice1 = references.get(idxs.subList(0, cvSize));
            Tensor<Double> slice2 = references.get(idxs.subList(cvSize, cvSize * 2));
            JensenShannonDriftResult[] jsdr = JensenShannon.calculatePerChannel(slice1, slice2, dummyThresholds, normalize);

            for (int channel = 0; channel < nChannels; channel++) {
                double jsStat = jsdr[channel].getjsStat();
                if (jsStat < minThreshold[channel]) {
                    minThreshold[channel] = jsStat;
                } else if (jsStat > maxThreshold[channel]) {
                    maxThreshold[channel] = jsStat;
                }
                avgThreshold[channel] += jsStat;
            }
        }

        JensenShannonBaseline[] baselines = new JensenShannonBaseline[nChannels];
        for (int channel = 0; channel < nChannels; channel++) {
            baselines[channel] = new JensenShannonBaseline(minThreshold[channel], maxThreshold[channel], avgThreshold[channel] / numCV);
        }
        return baselines;
    }
}
