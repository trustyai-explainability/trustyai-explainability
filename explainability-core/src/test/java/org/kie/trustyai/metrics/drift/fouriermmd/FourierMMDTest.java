package org.kie.trustyai.metrics.drift.fouriermmd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

public class FourierMMDTest {

    final String trainDataSetFileName = "/Users/jtray/git/trustyai-explainability/explainability-core/src/test/resources/train_ts_x.csv";
    final String validDataSetFileName = "/Users/jtray/git/trustyai-explainability/explainability-core/src/test/resources/valid_ts_x.csv";
    final String testDataSetFileName = "/Users/jtray/git/trustyai-explainability/explainability-core/src/test/resources/test_ts_x.csv";

    Dataframe trainDF;
    Dataframe validDF;
    Dataframe testDF;

    protected void setup() throws Exception {

        trainDF = readCSV(trainDataSetFileName);

        validDF = readCSV(validDataSetFileName);

        testDF = readCSV(testDataSetFileName);
    }

    private Dataframe readCSV(String fileName) throws FileNotFoundException, IOException {

        BufferedReader br = new BufferedReader(new FileReader(fileName));

        br.readLine(); // skip header line

        final List<Prediction> predictions = new ArrayList<Prediction>();

        while (true) {

            final String line = br.readLine();
            if (line == null) {
                break;
            }

            String[] values = line.split(",");
            assert values.length == 13;

            final Feature x1 = new Feature("X1", Type.NUMBER, new Value(Double.parseDouble(values[2])));
            final Feature x2 = new Feature("X2", Type.NUMBER, new Value(Double.parseDouble(values[3])));
            final Feature x3 = new Feature("X3", Type.NUMBER, new Value(Double.parseDouble(values[4])));
            final Feature x4 = new Feature("X4", Type.NUMBER, new Value(Double.parseDouble(values[5])));
            final Feature x5 = new Feature("X5", Type.NUMBER, new Value(Double.parseDouble(values[6])));
            final Feature x6 = new Feature("X6", Type.NUMBER, new Value(Double.parseDouble(values[7])));
            final Feature x7 = new Feature("X7", Type.NUMBER, new Value(Double.parseDouble(values[8])));
            final Feature x8 = new Feature("X8", Type.NUMBER, new Value(Double.parseDouble(values[9])));
            final Feature x9 = new Feature("X9", Type.NUMBER, new Value(Double.parseDouble(values[10])));
            final Feature x10 = new Feature("X10", Type.NUMBER, new Value(Double.parseDouble(values[11])));

            final List<Feature> features = new ArrayList<Feature>(
                    Arrays.asList(x1, x2, x3, x4, x5, x6, x7, x8, x9, x10));

            final PredictionInput predIn = new PredictionInput(features);
            final PredictionOutput predOut = new PredictionOutput(new ArrayList<Output>());
            final Prediction prediction = new SimplePrediction(predIn, predOut);

            predictions.add(prediction);
        }

        br.close();

        return Dataframe.createFrom(predictions);
    }

    @Test
    void testValidData() {
        try {
            setup();

            FourierMMD fourierMMD = new FourierMMD(trainDF);

            FourierMMDResult drift = fourierMMD.calculate(validDF);

            assert !drift.drift;

            assert drift.magnitude < 1.0;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testProductionData() {
        try {
            setup();

            FourierMMD fourierMMD = new FourierMMD(trainDF);

            FourierMMDResult drift = fourierMMD.calculate(testDF);

            assert drift.drift;

            assert drift.magnitude >= 1.0;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
