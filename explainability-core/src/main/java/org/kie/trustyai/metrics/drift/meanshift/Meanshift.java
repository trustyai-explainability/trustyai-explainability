package org.kie.trustyai.metrics.drift.meanshift;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.inference.TTest;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.utils.DataUtils;

public class Meanshift {
    private Map<String, StatisticalSummaryValues> fitStats;
    private final TTest tTest = new TTest();

    // fit from a specific dataframe
    public Meanshift(Dataframe dfTrain) {
        MeanshiftFitting meanshiftFitting = precompute(dfTrain);
        fitStats = meanshiftFitting.fitStats;
    }

    // fit if data stats are already known
    public Meanshift(MeanshiftFitting meanshiftFitting) {
        fitStats = meanshiftFitting.fitStats;
    }

    public static MeanshiftFitting precompute(Dataframe dfTrain) {
        List<Type> types = dfTrain.getColumnTypes();
        Map<String, StatisticalSummaryValues> computedStats = new HashMap<>();
        for (int i = 0; i < dfTrain.getColumnDimension(); i++) {
            // check that average + std have semantic meaning
            if (types.get(i).equals(Type.NUMBER)) {
                computedStats.put(dfTrain.getColumnNames().get(i), getColumnStats(dfTrain.getColumn(i)));
            }
        }

        return new MeanshiftFitting(computedStats);
    }

    public Map<String, MeanshiftResult> calculate(Dataframe dfTest, double alpha) {
        List<Type> types = dfTest.getColumnTypes();
        List<String> testNames = dfTest.getColumnNames();

        // all degs of freedom are the same for each column
        TDistribution tDistribution = new TDistribution(fitStats.values().iterator().next().getN() + dfTest.getRowDimension() - 2);

        HashMap<String, MeanshiftResult> result = new HashMap<>();
        for (int i = 0; i < dfTest.getColumnDimension(); i++) {
            // check that average + std have semantic meaning
            if (types.get(i).equals(Type.NUMBER)) {
                String colName = testNames.get(i);

                // validate df match   n
                if (!fitStats.containsKey(colName)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Passed dataframe not compatible with the mean-shift fitting: no such column in fitting with name %s.",
                                    testNames.get(i)));
                }

                StatisticalSummaryValues testStats = getColumnStats(dfTest.getColumn(i));
                double tStat = tTest.t(fitStats.get(colName), testStats);

                double pValue = (1 - tDistribution.cumulativeProbability(Math.abs(tStat))) * 2;
                boolean reject = pValue <= alpha;
                result.put(colName, new MeanshiftResult(tStat, pValue, reject));
            }
        }
        return result;
    }

    private static StatisticalSummaryValues getColumnStats(List<Value> column) {
        double[] colArray = column.stream().mapToDouble(Value::asNumber).toArray();
        double mean = DataUtils.getMean(colArray);
        double std = DataUtils.getStdDev(colArray, mean);
        return new StatisticalSummaryValues(mean, Math.pow(std, 2), colArray.length, 0, 0, 0);
    }

}
