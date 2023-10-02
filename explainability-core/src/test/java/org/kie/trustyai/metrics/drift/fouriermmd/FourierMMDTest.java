package org.kie.trustyai.metrics.drift.fouriermmd;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.drift.HypothesisTestResult;

public class FourierMMDTest {

    final String trainDataSetFileName = "train_ts_x.csv";
    final String validDataSetFileName = "valid_ts_x.csv";
    final String testDataSetFileName = "test_ts_x.csv";

    Dataframe trainDF;
    Dataframe validDF;
    Dataframe testDF;

    protected void setup() throws Exception {

        trainDF = readCSV(trainDataSetFileName);

        validDF = readCSV(validDataSetFileName);

        testDF = readCSV(testDataSetFileName);
    }

    protected static Dataframe generateRandomDataframe(int observations, int featureDiversity) {
        final List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random(0);
        for (int i = 0; i < observations; i++) {
            final List<Feature> featureList = List.of(
                    // guarantee feature diversity for age is min(observations, featureDiversity)
                    FeatureFactory.newNumericalFeature("age", i % featureDiversity),
                    FeatureFactory.newNumericalFeature("gender", random.nextBoolean() ? 1 : 0),
                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    protected static Dataframe generateRandomDataframeDrifted(int observations, int featureDiversity) {
        final List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random(0);
        for (int i = 0; i < observations; i++) {
            final List<Feature> featureList = List.of(
                    // guarantee feature diversity for age is min(observations, featureDiversity)
                    FeatureFactory.newNumericalFeature("age", (i % featureDiversity) + featureDiversity),
                    FeatureFactory.newNumericalFeature("gender", 0),
                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    private Dataframe readCSV(String fileName) throws FileNotFoundException, IOException {

        final InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
        final InputStreamReader isr = new InputStreamReader(is);
        final BufferedReader br = new BufferedReader(isr);

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

            final boolean deltaStat = true;
            final int n_test = 100;
            final int n_window = 168;
            final double sig = 10.0;
            final int randomSeed = 1234;
            final int n_mode = 512;
            final double epsilon = 1.0e-7;
            FourierMMD fourierMMD = new FourierMMD(trainDF, deltaStat, n_test, n_window, sig, randomSeed, n_mode,
                    epsilon);

            final double threshold = 0.8;
            final double gamma = 1.5;
            HypothesisTestResult drift = fourierMMD.calculate(validDF, threshold, gamma);

            Assertions.assertFalse(drift.isReject(), "drifted flag is true");

            Assertions.assertTrue(drift.getpValue() < 1.0, "drift.pValue >= 1.0");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testProductionData() {
        try {
            setup();

            final boolean deltaStat = true;
            final int n_test = 100;
            final int n_window = 168;
            final double sig = 10.0;
            final int randomSeed = 1234;
            final int n_mode = 512;
            final double epsilon = 1.0e-7;
            FourierMMD fourierMMD = new FourierMMD(trainDF, deltaStat, n_test, n_window, sig, randomSeed, n_mode,
                    epsilon);

            final double threshold = 0.8;
            final double gamma = 1.5;
            HypothesisTestResult drift = fourierMMD.calculate(testDF, threshold, gamma);

            Assertions.assertTrue(drift.isReject(), "drifted flag is false");

            Assertions.assertTrue(drift.getpValue() >= 1.0, "drift.pValue < 1.0");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testRandomData() {
        try {
            // def test_tab_prod_data(self):
            // window = 20
            // ss = Fourier_MMD(time_column=None, gamma=4, n_window=window,
            // delta_stat=False)

            // self.train_tab_x = self._generateRandomDataframe(100, 100)

            Dataframe trainTabX = FourierMMDTest.generateRandomDataframe(100, 100);

            // _ = ss.learn(self.train_tab_x)

            final boolean deltaStat = false;
            final int n_test = 100;
            final int n_window = 20;
            final double sig = 10.0;
            final int randomSeed = 1234;
            final int n_mode = 512;
            final double epsilon = 1.0e-7;
            FourierMMD fourierMMD = new FourierMMD(trainTabX, deltaStat, n_test, n_window, sig, randomSeed, n_mode,
                    epsilon);

            // d_res = ss.execute(self.test_tab_x)

            // self.test_tab_x = self._generateRandomDataframeDrifted(100,100)

            Dataframe testTabX = FourierMMDTest.generateRandomDataframeDrifted(100, 100);

            final double threshold = 0.8;
            final double gamma = 4.0;
            HypothesisTestResult drift = fourierMMD.calculate(testTabX, threshold, gamma);

            // self.assertIsNotNone(d_res)
            // self.assertIsInstance(d_res, dict)
            // self.assertListEqual(
            // list(d_res.keys()), ["drift", "magnitude", "computed_values"]
            // )
            // self.assertTrue(d_res["drift"])
            // self.assertGreaterEqual(d_res["magnitude"], 1)

            Assertions.assertTrue(drift.isReject(), "drifted flag is false");

            Assertions.assertTrue(drift.getpValue() >= 1.0, "drift.pValue < 1.0");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
