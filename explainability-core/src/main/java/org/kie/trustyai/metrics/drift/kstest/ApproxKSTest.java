package org.kie.trustyai.metrics.drift.kstest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.drift.HypothesisTestResult;
import org.kie.trustyai.metrics.drift.kstest.GKSketch.GKException;

/*
 * Implements Approximate Kolmogorov-Smirnov Test using Greenwald-Khanna epsilon sketch as described in A. Lall, “Data streaming algorithms for the kolmogorov-smirnov test”
in 2015 IEEE International Conference on Big Data (Big Data), 2015, pp. 95–104
 */
public class ApproxKSTest {
    public double eps = 0.01d; // sketch approximation
    private Map<String, GKSketch> trainGKSketches;

    public ApproxKSTest(
            double eps,
            Dataframe dfTrain) {
        this.eps = eps;
        //precompute GKSketch of training data
        ApproxKSFitting ksFitting = precompute(dfTrain, eps);
        trainGKSketches = ksFitting.getfitSketches();

    }

    // fit if GKSketch is already known
    public ApproxKSTest(double eps, ApproxKSFitting approxKSFitting) {
        trainGKSketches = approxKSFitting.getfitSketches();
        this.eps = eps;
    }

    public static ApproxKSFitting precompute(Dataframe dfTrain, double eps) {
        List<Type> types = dfTrain.getColumnTypes();
        Map<String, GKSketch> sketches = new HashMap<String, GKSketch>();

        for (int i = 0; i < dfTrain.getColumnDimension(); i++) {
            if (types.get(i).equals(Type.NUMBER)) {
                // build epsilon sketch for given column
                GKSketch sketch = new GKSketch(eps);
                dfTrain.getColumn(i).stream().mapToDouble(Value::asNumber).forEach(sketch::insert);
                sketches.put(dfTrain.getColumnNames().get(i), sketch);
            }
        }
        return new ApproxKSFitting(sketches);
    }

    /*
     * Returns HypothesisTestResult per column with Approximate KSTest statistic, p-value and reject
     */
    public HashMap<String, HypothesisTestResult> calculate(Dataframe dfTest, double signif) {
        List<Type> types = dfTest.getColumnTypes();
        List<String> testNames = dfTest.getColumnNames();

        HashMap<String, HypothesisTestResult> result = new HashMap<>();
        for (int i = 0; i < dfTest.getColumnDimension(); i++) {
            if (types.get(i).equals(Type.NUMBER)) {
                String colName = testNames.get(i);
                if (!trainGKSketches.containsKey(colName)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Passed dataframe not compatible with the train dataframe: no such column in train dataframe with name %s.",
                                    testNames.get(i)));
                }
                if (dfTest.getRowDimension() < 2 || trainGKSketches.get(colName).size() < 2) {
                    result.put(colName, new HypothesisTestResult(0, 1, false));
                } else {
                    GKSketch testSketch = new GKSketch(eps);
                    GKSketch trainSketch = trainGKSketches.get(colName);
                    dfTest.getColumn(i).stream().mapToDouble(Value::asNumber).forEach(testSketch::insert);
                    double d = 0.0;
                    try {
                        d = computeKSDistance(trainSketch, testSketch);
                    } catch (GKException e) {
                        throw new RuntimeException("Unexpected execution of GKSketch:  " + e.getMessage());
                    }
                    double pValue = computePvalue(d, trainSketch.getNumx(), testSketch.getNumx()); //compute pvalue
                    boolean reject = pValue <= signif;
                    result.put(colName, new HypothesisTestResult(d, pValue, reject));
                }
            }
        }

        return result;
    }

    private double computePvalue(double d, int numx, int numx2) {
        // Compute p-value from max distance D
        KolmogorovSmirnovTest exact_ks = new KolmogorovSmirnovTest();
        double pval = exact_ks.approximateP(d, numx, numx2);

        return pval;
    }

    private double computeKSDistance(GKSketch trainSketch, GKSketch testSketch) throws GKException {
        // Lall's Two Sample KS algorithm using GK sketch. Algorithm 2 in [Lall 2015]
        double[] trainSketchValues = trainSketch.getSummary()
                .stream().mapToDouble(Triple::getLeft).toArray();
        double[] testSketchValues = testSketch.getSummary()
                .stream().mapToDouble(Triple::getLeft).toArray();

        double maxD = 0.0;
        Set<Double> mergedSketches = new HashSet<Double>();
        for (double val : trainSketchValues) {
            mergedSketches.add(val);
        }
        for (double val : testSketchValues) {
            mergedSketches.add(val);
        }
        int trainSize = trainSketch.getNumx();
        int testSize = testSketch.getNumx();

        for (double v : mergedSketches) {

            int trainApproxRank = trainSketch.rank(v);
            int testApproxRank = testSketch.rank(v);

            double trainApproxProb = (double) trainApproxRank / trainSize;
            double testApproxProb = (double) testApproxRank / testSize;
            double vDist = Math.abs(trainApproxProb - testApproxProb);
            maxD = Math.max(vDist, maxD);

        }

        return maxD;
    }

    @Override
    public String toString() {
        return "ApproxKSTest [eps=" + eps + ", trainGKSketches=" + trainGKSketches + "]";
    }

}
