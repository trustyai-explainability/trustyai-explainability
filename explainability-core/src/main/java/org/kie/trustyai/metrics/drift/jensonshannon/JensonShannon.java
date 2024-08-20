package org.kie.trustyai.metrics.drift.jensonshannon;

import java.util.Arrays;

import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.statistics.distance.KLDivergence;

/**
 * Jensen-Shannon divergence to calculate image data drift.
 * See <a href="https://en.wikipedia.org/wiki/Jensen%E2%80%93Shannon_divergence">Jensen–Shannon divergence</a>
 */
public class JensonShannon {
    /**
     * Calculates the Jenson-Shannon divergence between two pixel arrays.
     *
     * @param p1 The reference pixel array.
     * @param p2 The hypothesis pixel array.
     * @return The Jenson-Shannon divergence.
     */
    public static double jensonShannonDivergence(Double[] p1, Double[] p2) {
        Double[] m = new Double[p1.length];
        for (int i = 0; i < p1.length; i++) {
            m[i] = (p1[i] + p2[i]) / 2.0;
        }
        KLDivergence kl_div1 = new KLDivergence(p1, m);
        KLDivergence kl_div2 = new KLDivergence(p2, m);
        return (kl_div1.calculate() + kl_div2.calculate()) / 2.0;
    }

    /**
     * Calculates the average Jensen-Shannon divergence over channel values between reference and hypothesis tensors.
     * If it is above the threshold, the hypothesis tensor is said to be statistically different than the reference tensor.
     *
     * @param references The reference image.
     * @param hypotheses The hypothesis image.
     * @param threshold A threshold to determine whether the hypothesis image is different from the reference image.
     * @return The image drift result.
     */

    public static JensonShannonDriftResult calculate(Tensor<Double> references, Tensor<Double> hypotheses, double threshold) {
        return JensonShannon.calculate(references, hypotheses, threshold, false);
    }

    /**
     * Calculates the average Jensen-Shannon divergence over channel values between reference and hypothesis tensors.
     * If it is above the threshold, the hypothesis tensor is statistically different from the reference tensor.
     *
     * @param references The reference image.
     * @param hypotheses The hypothesis image.
     * @param threshold A threshold to determine whether the hypothesis image is different from the reference image.
     * @param normalize: whether to normalize via the reference tensor size: this will keep JS results consistent regardless of the number of references
     *
     * @return a JensonShannonDriftResult, containing the computed Jenson-Shannon statistic and whether the threshold is violated.
     */

    public static JensonShannonDriftResult calculate(Tensor<Double> references, Tensor<Double> hypotheses, double threshold, boolean normalize) {
        if (!Arrays.equals(references.getDimensions(), hypotheses.getDimensions())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Dimensions of references (%s) and hypotheses (%s) do not match",
                            Arrays.toString(references.getDimensions()),
                            Arrays.toString(hypotheses.getDimensions())));
        }

        int nChannels = references.getDimensions(1);
        int normalizationFactor = references.getnEntries();

        double jsStat = 0.0;
        for (int channel = 0; channel < nChannels; channel++) {
            Tensor<Double> referenceSlice = references.getFromSecondAxis(channel);
            Tensor<Double> hypothesisSlice = hypotheses.getFromSecondAxis(channel);
            jsStat += jensonShannonDivergence(referenceSlice.getData(), hypothesisSlice.getData());
        }

        if (normalize) {
            jsStat /= normalizationFactor;
        }

        boolean reject = jsStat > threshold;
        return new JensonShannonDriftResult(jsStat, threshold, reject);
    }

    /**
     * Calculates the Jensen-Shannon divergence for each channel between reference and hypothesis tensors.
     * If a particular channel is above the threshold, that channel of the hypothesis tensor is statistically different from the equivalent channel of the reference tensor.
     *
     * @param references The reference image.
     * @param hypotheses The hypothesis image.
     * @param threshold The per-channel thresholds to determine whether the hypothesis channel is different from the reference channel.
     * @param normalize: whether to normalize via the per-channel tensor size: this will keep JS results consistent regardless of the number of references
     *
     * @return an array of JensonShannonDriftResult, where the ith element contains the computed Jenson-Shannon statistic for the ith channel and whether the threshold is violated.
     */

    public static JensonShannonDriftResult[] calculatePerChannel(Tensor<Double> references, Tensor<Double> hypotheses, double[] threshold, boolean normalize) {
        if (!Arrays.equals(references.getDimensions(), hypotheses.getDimensions())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Dimensions of references (%s) and hypotheses (%s) do not match",
                            Arrays.toString(references.getDimensions()),
                            Arrays.toString(hypotheses.getDimensions())));
        }

        int nChannels = references.getDimensions(1);

        JensonShannonDriftResult[] results = new JensonShannonDriftResult[nChannels];
        for (int channel = 0; channel < nChannels; channel++) {
            Tensor<Double> referenceSlice = references.getFromSecondAxis(channel);
            Tensor<Double> hypothesisSlice = hypotheses.getFromSecondAxis(channel);
            double jsStat = jensonShannonDivergence(referenceSlice.getData(), hypothesisSlice.getData());
            if (normalize) {
                jsStat /= referenceSlice.getnEntries();
            }
            boolean reject = jsStat > threshold[channel];
            results[channel] = new JensonShannonDriftResult(jsStat, threshold[channel], reject);
        }
        return results;
    }
}
