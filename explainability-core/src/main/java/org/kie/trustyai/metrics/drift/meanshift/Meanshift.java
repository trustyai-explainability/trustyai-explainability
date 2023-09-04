package org.kie.trustyai.metrics.drift.meanshift;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.inference.TTest;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.utils.DataUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Meanshift {
    private Map<Integer, StatisticalSummaryValues> fitStats;
    private Map<Integer, String> fitNames;
    private final TTest tTest = new TTest();


    // fit from a specific dataframe
    public Meanshift(Dataframe dfTrain){
        List<Type> types = dfTrain.getColumnTypes();
        for (int i=0; i<dfTrain.getColumnDimension(); i++){
            fitNames.put(i, dfTrain.getColumnNames().get(i));

            // check that average + std have semantic meaning
            if (types.get(i).equals(Type.NUMBER)){
                fitStats.put(i, getColumnStats(dfTrain.getColumn(i)));
            }
        }
    }

    // fit if data stats are already known
    public Meanshift(Map<Integer, StatisticalSummaryValues> fitStats, Map<Integer, String> fitNames){
        this.fitStats = fitStats;
        this.fitNames = fitNames;
    }

    public Map<Integer, MeanshiftResult> calculate(Dataframe dfTest, double alpha){
        List<Type> types = dfTest.getColumnTypes();
        List<String> testNames = dfTest.getColumnNames();

        HashMap<Integer, MeanshiftResult> result = new HashMap<>();
        for (int i=0; i<dfTest.getColumnDimension(); i++){

            // check that average + std have semantic meaning
            if (types.get(i).equals(Type.NUMBER)){

                // validate df match   n
                if (!fitNames.get(i).equals(testNames.get(i))){
                    throw new IllegalArgumentException(
                            String.format(
                                    "Passed dataframe not compatible with the mean-shift fitting: expected column at index %d to have the name %s, got %s instead.",
                                    i, fitNames.get(i), testNames.get(i)
                            )
                    );
                }

                StatisticalSummaryValues testStats = getColumnStats(dfTest.getColumn(i));
                double t_stat = tTest.t(fitStats.get(i), testStats);
                double p_value = tTest.t(fitStats.get(i), testStats);
                boolean reject = p_value <= alpha;

                result.put(i, new MeanshiftResult(t_stat, p_value, reject));
            }
        }
        return result;
    }

    private StatisticalSummaryValues getColumnStats(List<Value> column){
        double[] colArray = column.stream().mapToDouble(Value::asNumber).toArray();
        double mean = DataUtils.getMean(colArray);
        double std = DataUtils.getStdDev(colArray, mean);
        return new StatisticalSummaryValues(mean, Math.pow(std, 2), colArray.length, 0, 0, 0);
    }

}
