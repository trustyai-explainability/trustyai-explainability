package org.kie.trustyai.metrics.drift.jensonshannon;

import java.util.Arrays;

import org.kie.trustyai.explainability.model.tensor.Slice;
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
     * If it is above the threshold, the hypothesis tensor is said to be statistically different than the reference tensor.
     *
     * @param references The reference image.
     * @param hypotheses The hypothesis image.
     * @param threshold A threshold to determine whether the hypothesis image is different from the reference image.
     * @param normalize: whether to normalize via the total element count of reference tensor (product of all dimensions)
     * @return The image drift result.
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
            Tensor<Double> referenceSlice = references.slice(Slice.all(), Slice.at(channel));
            Tensor<Double> hypothesisSlice = hypotheses.slice(Slice.all(), Slice.at(channel));
            jsStat += jensonShannonDivergence(referenceSlice.getData(), hypothesisSlice.getData());
        }

        if (normalize) {
            jsStat /= normalizationFactor;
        }

        boolean reject = jsStat > threshold;
        return new JensonShannonDriftResult(jsStat, threshold, reject);
    }
}
