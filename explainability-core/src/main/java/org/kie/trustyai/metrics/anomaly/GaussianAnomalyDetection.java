package org.kie.trustyai.metrics.anomaly;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.utils.PerColumnStatisticalAnalysis;
import org.kie.trustyai.metrics.utils.PerColumnStatistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class GaussianAnomalyDetection extends PerColumnStatisticalAnalysis {
    /**
     * Fit the Gaussian Anomaly Detector to a training dataset, computing the mean and standard deviation for each numeric feature and output.
     *
     * @param dfTrain: the Dataframe over which to calculate per-column means and standard deviations
     */
    public GaussianAnomalyDetection(Dataframe dfTrain) {
        super(dfTrain);
    }

    /**
     * Fit the Gaussian Anomaly Detector to a training dataset, from precomputed means and standard deviations for each feature and output.
     *
     * @param perColumnStatistics: Precomputed per-column means and standard deviations
     */
    public GaussianAnomalyDetection(PerColumnStatistics perColumnStatistics){
        super(perColumnStatistics);
    }


    /**
     * Compute a bounded probability (see below) with a window size of 1 standard deviation.
     *
     * @param testPrediction: the data point over which to compute the per-column probabilities
     * @return a map of column name : probabilities
     */
    public Map<String, Double> calculateBoundedProbability(Prediction testPrediction){
        return calculateBoundedProbability(testPrediction, 1.);
    }

    /**
     * Find the probability of drawing each feature/output value plus/minus a configurable window from the Gaussian column distributions.
     * That is, for a feature/output value f, window size w, and computed standard deviation of the corresponding column s, this finds the probability of drawing a point
     * f - sw < x <= f + sw from the column distribution. This describes how likely values within $w$ standard deviations of f are to come from the fit distribution.
     * Values close to 1 indicate that f is well within the column's calculated Gaussian distribution, while numbers close to 0 indicate that f is far from the distribution.
     *
     * @param testPrediction: the data point over which to compute the per-column probabilities
     * @param windowSize: the number of standard deviations on each side of the feature/output value to use
     * @return a map of column name : probabilities
     */
    public Map<String, Double> calculateBoundedProbability(Prediction testPrediction, double windowSize) {
        BiFunction<StatisticalSummaryValues, Double, Double> calculation = (ssv, featureValue) -> {
            NormalDistribution normalDistribution = new NormalDistribution(ssv.getMean(), ssv.getStandardDeviation());
            double window = windowSize * ssv.getStandardDeviation();
            return normalDistribution.probability(featureValue - window, featureValue + window);
        };
        return perColumnCalculation(testPrediction, calculation);
    }

    /**
     * Compute a normalized bounded probability (see below) with a window size of 1 standard deviation.
     *
     * @param testPrediction: the data point over which to compute the per-column probabilities
     * @return a map of column name : probabilities
     */
    public Map<String, Double> calculateNormalizedBoundedProbability(Prediction testPrediction) {
        return calculateNormalizedBoundedProbability(testPrediction, 1.);
    }

    /**
     * Find the normalized probability of drawing each feature/output value plus/minus a configurable window from the Gaussian column distributions.
     * That is, for a feature/output value f, window size w, column mean u, and column standard deviations, this computes
     * (f - sw < x <= f + sw)/(u - sw < x <= u + sw)
     * This describes how likely values within $w$ standard deviations of f are to come from the fit distribution. Values close to 1 indicate that f is close to the mean of the
     * distribution, while numbers close to 0 indicate that f is far from the distribution.
     *
     * @param testPrediction: the data point over which to compute the per-column probabilities
     * @param windowSize: the number of standard deviations on each side of the feature/output value to use
     * @return a map of column name : probabilities
     */
    public Map<String, Double> calculateNormalizedBoundedProbability(Prediction testPrediction, double windowSize) {
        BiFunction<StatisticalSummaryValues, Double, Double> calculation = (ssv, featureValue) -> {
            NormalDistribution normalDistribution = new NormalDistribution(ssv.getMean(), ssv.getStandardDeviation());

            double window = windowSize * ssv.getStandardDeviation();
            return normalDistribution.probability(featureValue - window, featureValue + window)/
                    normalDistribution.probability(ssv.getMean() - window, ssv.getMean() + window);
        };
        return perColumnCalculation(testPrediction, calculation);
    }

    /**
     * Compute the normal deviation of each feature/output value in the test prediction:
     * (x-u)/s, where u is the column mean and s is the column standard deviation. This gives the number of standard
     * deviations each feature/output value is from their respective column means.
     *
     * @param testPrediction: the data point over which to compute the per-column normalized deviation
     * @return a map of column name : normalized deviation
     */
    public Map<String, Double> calculateNormalizedDeviation(Prediction testPrediction){
        BiFunction<StatisticalSummaryValues, Double, Double> calculation =
                (ssv, featureValue) -> (featureValue - ssv.getMean())/ssv.getStandardDeviation();
        return perColumnCalculation(testPrediction, calculation);
    }

    /**
     * Helper function to compute some column-analysis function over each column in the testPrediction
     *
     * @param testPrediction: The prediction over which to compute per-column analysis
     * @param calculation: Some function f(columnStats, columnValue) -> double
     * @return map column name : function output
     */
    private Map<String, Double> perColumnCalculation(Prediction testPrediction, BiFunction<StatisticalSummaryValues, Double, Double> calculation) {
        List<String> testNames = new ArrayList<>();
        List<Type> types = new ArrayList<>();
        List<Value> values = new ArrayList<>();
        for (Feature f : testPrediction.getInput().getFeatures()){
            testNames.add(f.getName());
            types.add(f.getType());
            values.add(f.getValue());
        }
        for (Output o : testPrediction.getOutput().getOutputs()){
            testNames.add(o.getName());
            types.add(o.getType());
            values.add(o.getValue());
        }

        // all degs of freedom are the same for each column
        HashMap<String, Double> result = new HashMap<>();
        for (int i = 0; i < testNames.size(); i++) {
            // check that average + std have semantic meaning
            if (types.get(i).equals(Type.NUMBER)) {
                String colName = testNames.get(i);

                // validate df match   n
                if (!this.getFitStats().containsKey(colName)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Passed dataframe not compatible with the Gaussian fitting: no such column in fitting with name %s.",
                                    testNames.get(i)));
                }

                StatisticalSummaryValues ssv = getFitStats().get(colName);
                double featureValue = values.get(i).asNumber();
                result.put(colName,calculation.apply(ssv, featureValue));
            }
        }
        return result;
    }
}
