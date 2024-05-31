package org.kie.trustyai.metrics.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.utils.DataUtils;

// defines logic for calculating statistical summaries values for each column in a dataframe, to be used to analyze distributions, etc
public class PerColumnStatisticalAnalysis {
    private Map<String, StatisticalSummaryValues> fitStats;

    public PerColumnStatisticalAnalysis(Dataframe dfTrain) {
        fitStats = precompute(dfTrain).fitStats;
    }

    public PerColumnStatisticalAnalysis(PerColumnStatistics perColumnStatistics) {
        fitStats = perColumnStatistics.fitStats;
    }

    public static PerColumnStatistics precompute(Dataframe dfTrain) {
        List<Type> types = dfTrain.getColumnTypes();
        Map<String, StatisticalSummaryValues> computedStats = new HashMap<>();
        for (int i = 0; i < dfTrain.getColumnDimension(); i++) {
            // check that average + std have semantic meaning
            if (types.get(i).equals(Type.NUMBER)) {
                computedStats.put(dfTrain.getRawColumnNames().get(i), getColumnStats(dfTrain.getColumn(i)));
            }
        }

        return new PerColumnStatistics(computedStats);
    }

    public Map<String, StatisticalSummaryValues> getFitStats() {
        return Collections.unmodifiableMap(fitStats);
    }

    protected static StatisticalSummaryValues getColumnStats(List<Value> column) {
        double[] colArray = column.stream().mapToDouble(Value::asNumber).toArray();
        double mean = DataUtils.getMean(colArray);
        double std = DataUtils.getStdDev(colArray, mean);
        return new StatisticalSummaryValues(mean, Math.pow(std, 2), colArray.length, 0, 0, 0);
    }

}
