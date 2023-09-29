package org.kie.trustyai.metrics.drift.kstest;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.math3.stat.inference.KolmogorovSmirnovTest;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.drift.HypothesisTestResult;

public class KSTest {
    public KSTest() {

    }

    /*
     * Returns HypothesisTestResult per column with KSTest statistic, p-value and reject
     */
    public HashMap<String, HypothesisTestResult> calculate(Dataframe dfTrain, Dataframe dfTest, double signif) {
        double d = 0.0d;
        List<Type> types = dfTrain.getColumnTypes();
        List<String> trainNames = dfTrain.getColumnNames();
        List<String> testNames = dfTest.getColumnNames();

        HashMap<String, HypothesisTestResult> result = new HashMap<>();
        for (int i = 0; i < dfTest.getColumnDimension(); i++) {
            if (types.get(i).equals(Type.NUMBER)) {
                String colName = testNames.get(i);
                if (!trainNames.contains(colName)) {
                    throw new IllegalArgumentException(
                            String.format(
                                    "Passed dataframe not compatible with the train dataframe: no such column in train dataframe with name %s.",
                                    testNames.get(i)));
                }
                if (dfTrain.getRowDimension() < 2 || (dfTest.getRowDimension() < 2)) {
                    result.put(colName, new HypothesisTestResult(0, 1, false));
                } else {
                    double[] trainArray = dfTrain.getColumn(i).stream().mapToDouble(Value::asNumber).toArray();
                    double[] testArray = dfTest.getColumn(i).stream().mapToDouble(Value::asNumber).toArray();

                    KolmogorovSmirnovTest ks_test = new KolmogorovSmirnovTest();
                    d = ks_test.kolmogorovSmirnovStatistic(trainArray, testArray);
                    double pValue = ks_test.kolmogorovSmirnovTest(trainArray, testArray);
                    boolean reject = pValue <= signif;
                    result.put(colName, new HypothesisTestResult(d, pValue, reject));
                }
            }
        }
        return result;
    }

}
