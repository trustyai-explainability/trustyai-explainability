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
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMDResult;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMD;

// class Test_Fourier_MMD(unittest.TestCase):
// def setUp(self):
//     data_path = "data/data_for_units/classification"
//     # read data
//     self.ts_x = pd.read_csv(os.path.join(data_path, "ts_x.csv"))

//     self.asset_id = "asset_id"
//     self.date_col = "dateofsensordata"
//     self.date_format = "%Y-%m-%d %H:%M:%S"

//     split_time_range = {
//         "train": ("1990-01-01", "1990-06-30"),
//         "valid": ("1990-07-01", "1990-08-31"),
//         "test": ("1991-01-01", "1991-02-28"),
//     }

//     result = split_data_using_time_range(
//         dataset=self.ts_x,
//         timecolumn=self.date_col,
//         time_ranges=split_time_range,
//         timeformat=self.date_format,
//         group_columns=[self.asset_id],
//         parse_as_str=True,
//     )
//     self.train_ts_x = result.__next__()
//     self.valid_ts_x = result.__next__()
//     self.test_ts_x = result.__next__()

// def test_valid_data(self):
//     ss = Fourier_MMD(time_column=self.date_col)
//     selected_feature = self.train_ts_x.columns.tolist()
//     selected_feature.remove(self.asset_id)
//     ss.learn(self.train_ts_x[selected_feature])
//     d_res = ss.execute(self.valid_ts_x[selected_feature])
//     self.assertIsNotNone(d_res)
//     self.assertIsInstance(d_res, dict)
//     self.assertListEqual(
//         list(d_res.keys()), ["drift", "magnitude", "computed_values"]
//     )
//     self.assertFalse(d_res["drift"])
//     self.assertLess(d_res["magnitude"], 1)

// def test_prod_data(self):
//     ss = Fourier_MMD(time_column=self.date_col)
//     selected_feature = self.train_ts_x.columns.tolist()
//     selected_feature.remove(self.asset_id)
//     ss.learn(self.train_ts_x[selected_feature])
//     d_res = ss.execute(self.test_ts_x[selected_feature])
//     self.assertIsNotNone(d_res)
//     self.assertIsInstance(d_res, dict)
//     self.assertListEqual(
//         list(d_res.keys()), ["drift", "magnitude", "computed_values"]
//     )
//     self.assertTrue(d_res["drift"])
//     self.assertGreaterEqual(d_res["magnitude"], 1)

public class FourierMMDTest {

    final String trainDataSetFileName = "/Users/jtray/git/trustyai-explainability/explainability-core/src/test/resources/train_ts_x.csv";
    final String validDataSetFileName = "/Users/jtray/git/trustyai-explainability/explainability-core/src/test/resources/valid_ts_x.csv";
    final String testDataSetFileName = "/Users/jtray/git/trustyai-explainability/explainability-core/src/test/resources/test_ts_x.csv";

    Dataframe trainDF;
    Dataframe validDF;
    Dataframe testDF;

    // def setUp(self):
    // data_path = "data/data_for_units/classification"
    // # read data
    // self.ts_x = pd.read_csv(os.path.join(data_path, "ts_x.csv"))

    // self.asset_id = "asset_id"
    // self.date_col = "dateofsensordata"
    // self.date_format = "%Y-%m-%d %H:%M:%S"

    // split_time_range = {
    // "train": ("1990-01-01", "1990-06-30"),
    // "valid": ("1990-07-01", "1990-08-31"),
    // "test": ("1991-01-01", "1991-02-28"),
    // }

    // result = split_data_using_time_range(
    // dataset=self.ts_x,
    // timecolumn=self.date_col,
    // time_ranges=split_time_range,
    // timeformat=self.date_format,
    // group_columns=[self.asset_id],
    // parse_as_str=True,
    // )
    // self.train_ts_x = result.__next__()
    // self.valid_ts_x = result.__next__()
    // self.test_ts_x = result.__next__()

    protected void setup() throws Exception {

        // ,dateofsensordata,X1,X2,X3,X4,X5,X6,X7,X8,X9,X10,asset_id

        // 0,1990-01-01
        // 00:00:00,-0.8764345329168759,1.05842523442413,-0.2641601211142639,4.2112000661412,-1.1024810400573,-1.08691853263879,
        // -1.37069372997094,1.59875655252833,-0.783603009204144,-0.0815666088067087,1

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

            System.out.println(trainDF.toString());

            FourierMMD fourierMMD = new FourierMMD(trainDF);

            FourierMMDResult drift = fourierMMD.calculate(validDF);

            // self.assertIsNotNone(d_res)
            // self.assertIsInstance(d_res, dict)
            // self.assertListEqual(
            // list(d_res.keys()), ["drift", "magnitude", "computed_values"]
            // )
            // self.assertFalse(d_res["drift"])

            System.out.println("drift = " + drift);

            assert !drift.drift;

            // self.assertLess(d_res["magnitude"], 1)

            assert drift.magnitude < 1.0;

            System.out.println("*** test passwd ***");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testProductionData() {
        // def test_prod_data(self):
        // ss = Fourier_MMD(time_column=self.date_col)
        // selected_feature = self.train_ts_x.columns.tolist()
        // selected_feature.remove(self.asset_id)
        // ss.learn(self.train_ts_x[selected_feature])
        // d_res = ss.execute(self.test_ts_x[selected_feature])
        // self.assertIsNotNone(d_res)
        // self.assertIsInstance(d_res, dict)
        // self.assertListEqual(
        // list(d_res.keys()), ["drift", "magnitude", "computed_values"]
        // )
        // self.assertTrue(d_res["drift"])
        // self.assertGreaterEqual(d_res["magnitude"], 1)

        try {
            setup();

            System.out.println(trainDF.toString());

            FourierMMD fourierMMD = new FourierMMD(trainDF);

            FourierMMDResult drift = fourierMMD.calculate(testDF);

            // self.assertIsNotNone(d_res)
            // self.assertIsInstance(d_res, dict)
            // self.assertListEqual(
            // list(d_res.keys()), ["drift", "magnitude", "computed_values"]
            // )
            // self.assertFalse(d_res["drift"])

            System.out.println("drift = " + drift);

            // self.assertTrue(d_res["drift"])

            assert drift.drift;

            // self.assertGreaterEqual(d_res["magnitude"], 1)

            assert drift.magnitude >= 1.0;

            System.out.println("*** test passwd ***");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
