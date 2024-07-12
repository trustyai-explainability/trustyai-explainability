package org.kie.trustyai.metrics.drift.meanshift;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.inference.TTest;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.metrics.utils.PerColumnStatisticalAnalysis;
import org.kie.trustyai.metrics.utils.PerColumnStatistics;

public class Meanshift extends PerColumnStatisticalAnalysis {
    private final TTest tTest = new TTest();

    // fit from a specific dataframe
    public Meanshift(Dataframe dfTrain) {
        super(dfTrain);
    }

    // use pre-computed fitting
    public Meanshift(PerColumnStatistics perColumnStatistics) {
        super(perColumnStatistics);
    }

    public Map<String, MeanshiftResult> calculate(Dataframe dfTest, double alpha) {
        List<Type> types = dfTest.getColumnTypes();
        List<String> testNames = dfTest.getRawColumnNames();

        // all degs of freedom are the same for each column
        TDistribution tDistribution = new TDistribution(this.getFitStats().values().iterator().next().getN() + dfTest.getRowDimension() - 2);

        HashMap<String, MeanshiftResult> result = new HashMap<>();
        for (int i = 0; i < dfTest.getColumnDimension(); i++) {
            // check that average + std have semantic meaning
            if (types.get(i).equals(Type.NUMBER)) {
                String colName = testNames.get(i);

                // validate df match   n
                if (!this.getFitStats().containsKey(colName)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Passed dataframe not compatible with the mean-shift fitting: no such column in fitting with name %s.",
                                    testNames.get(i)));
                }

                if (dfTest.getRowDimension() < 2) {
                    result.put(colName, new MeanshiftResult(0, 1, false));
                } else {
                    StatisticalSummaryValues testStats = getColumnStats(dfTest.getColumn(i));
                    double tStat = tTest.t(this.getFitStats().get(colName), testStats);
                    double pValue = (1 - tDistribution.cumulativeProbability(Math.abs(tStat))) * 2;
                    boolean reject = pValue <= alpha;
                    result.put(colName, new MeanshiftResult(tStat, pValue, reject));
                }
            }
        }
        return result;
    }
}
