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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaussianAnomalyDetection extends PerColumnStatisticalAnalysis {
    // fit from a specific dataframe
    public GaussianAnomalyDetection(Dataframe dfTrain) {
        super(dfTrain);
    }

    // use pre-computed fitting
    public GaussianAnomalyDetection(PerColumnStatistics perColumnStatistics){
        super(perColumnStatistics);
    }


    public Map<String, Double> calculate(Prediction testPrediction) {
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
                                    "Passed dataframe not compatible with the mean-shift fitting: no such column in fitting with name %s.",
                                    testNames.get(i)));
                }

                StatisticalSummaryValues ssv = getFitStats().get(colName);
                NormalDistribution normalDistribution = new NormalDistribution(ssv.getMean(), ssv.getStandardDeviation());
                result.put(colName, normalDistribution.density(values.get(i).asNumber()));
            }
        }
        return result;
    }
}
