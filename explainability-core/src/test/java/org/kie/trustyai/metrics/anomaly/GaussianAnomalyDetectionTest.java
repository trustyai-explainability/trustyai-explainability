package org.kie.trustyai.metrics.anomaly;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.metrics.drift.meanshift.Meanshift;
import org.kie.trustyai.metrics.utils.PregeneratedNormalData;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GaussianAnomalyDetectionTest {

    double[][] cols = PregeneratedNormalData.getData();
    int nReferenceCols = cols.length;

    @Test
    void testTTestOneColumnSingleRow() {
        for (int i = 0; i < nReferenceCols; i++) {
            for (int j = 0; j < nReferenceCols; j++) {
                Dataframe df1 = PregeneratedNormalData.generate(i);
                Dataframe df2 = PregeneratedNormalData.generate(j);
                Prediction p = new SimplePrediction(new PredictionInput(df2.getInputRowAsFeature(0)), new PredictionOutput(df2.getOutputRowAsOutput(0)));

                GaussianAnomalyDetection gad = new GaussianAnomalyDetection(df1);
                System.out.println("======");
                System.out.println("mean= "+ gad.getFitStats().get("0").getMean()+" std= "+ gad.getFitStats().get("0").getStandardDeviation());
                Map<String, Double> gadResults = gad.calculate(p);

                System.out.println(i + " : " + j + " : " + p);
                System.out.println(gadResults);
            }
        }
    }
}