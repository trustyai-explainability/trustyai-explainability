package org.kie.trustyai.explainability.local.tssaliency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureImportance;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.Saliency;
import org.kie.trustyai.explainability.model.SaliencyResults;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

public class TSSaliencyExplainerTest {

    @Test
    public void test1() {
        try {
            List<Feature> data = load();

            List<PredictionInput> inputs = new ArrayList<PredictionInput>(data.size());

            int count = 0;
            for (Feature datum : data) {

                assert datum.getType() == Type.VECTOR;

                Feature[] features2 = (Feature[]) datum.getValue().getUnderlyingObject();

                List<Feature> features2List = Arrays.asList(features2);

                PredictionInput input = new PredictionInput(features2List);
                inputs.add(input);

                if (++count == 2) {
                    break;
                }
            }

            PredictionProvider model = new TSSaliencyModel();

            CompletableFuture<List<PredictionOutput>> result = model.predictAsync(inputs);
            List<PredictionOutput> results = result.get();

            for (PredictionOutput prediction : results) {
                List<Output> outList = prediction.getOutputs();

                for (Output output : outList) {
                    System.out.println("*********** " + output);
                }
            }

            // ******** NOTE need a loop for e.g., Ford data

            PredictionInput predictionInput = inputs.get(0);

            PredictionOutput predictionOutput = results.get(0);

            UUID uuid = UUID.randomUUID();

            Prediction prediction = new SimplePrediction(predictionInput, predictionOutput, uuid);

            TSSaliencyExplainer explainer = new TSSaliencyExplainer(new double[0], 50, 1000, 0);

            CompletableFuture<SaliencyResults> saliencyResultsCompletable = explainer.explainAsync(prediction, model,
                    null);
            SaliencyResults saliencyResults = saliencyResultsCompletable.get();

            printResults(saliencyResults);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        try {
            List<Feature> data = loadClimate();

            List<PredictionInput> inputs = new ArrayList<PredictionInput>(data.size());

            int count = 0;
            for (Feature datum : data) {

                assert datum.getType() == Type.VECTOR;

                Feature[] features2 = (Feature[]) datum.getValue().getUnderlyingObject();

                List<Feature> features2List = Arrays.asList(features2);

                PredictionInput input = new PredictionInput(features2List);
                inputs.add(input);

            }

            PredictionProvider model = new TSSaliencyModel();

            CompletableFuture<List<PredictionOutput>> result = model.predictAsync(inputs);
            List<PredictionOutput> results = result.get();

            for (PredictionOutput prediction : results) {
                List<Output> outList = prediction.getOutputs();

                for (Output output : outList) {
                    System.out.println("*********** " + output);
                }
            }

            // ******** NOTE need a loop for e.g., Ford data

            PredictionInput predictionInput = inputs.get(0);

            PredictionOutput predictionOutput = results.get(0);

            UUID uuid = UUID.randomUUID();

            Prediction prediction = new SimplePrediction(predictionInput, predictionOutput, uuid);

            TSSaliencyExplainer explainer = new TSSaliencyExplainer(new double[0], 1000, 50, 0);

            CompletableFuture<SaliencyResults> saliencyResultsCompletable = explainer.explainAsync(prediction, model,
                    null);
            SaliencyResults saliencyResults = saliencyResultsCompletable.get();

            printResults(saliencyResults);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void printResults(SaliencyResults saliencyResults) {
        Map<String, Saliency> saliencyMap = saliencyResults.getSaliencies();
        Saliency saliency = saliencyMap.get("result");
        List<FeatureImportance> featureImportances = saliency.getPerFeatureImportance();
        FeatureImportance featureImportance = featureImportances.get(0);

        double[][] scoreResult = featureImportance.getScoreMatrix();

        System.out.println("Saliency results:");

        System.out.println("[");

        for (int t = 0; t < scoreResult.length; t++) {

            System.out.print("[");

        
            for (int f = 0; f < scoreResult[0].length; f++) {
                double score = scoreResult[t][f];
                System.out.print(score + ",");
            }

            System.out.println("],");            
        }

        System.out.println("]");

        System.out.println("- 30 -");
    }

    public List<Feature> load() throws Exception {

        File ifile = new File("../ford/FordA_TEST.txt");
        FileInputStream fis = new FileInputStream(ifile);
        InputStreamReader is = new InputStreamReader(fis);
        BufferedReader reader = new BufferedReader(is);

        try {
            List<Feature> features = new LinkedList<Feature>();

            int row = 0;
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                StringTokenizer st = new StringTokenizer(line, " ");

                List<Feature> features2 = new LinkedList<Feature>();

                int element = 0;
                while (st.hasMoreElements()) {
                    String value = (String) st.nextElement();
                    if (element > 0) {
                        double[] feature3Array = new double[1];
                        feature3Array[0] = Double.valueOf(value);
                        Feature feature3 = new Feature("element" + element, Type.VECTOR, new Value(feature3Array));
                        features2.add(feature3);

                        if (row == 0) {
                            System.out.print(value + ",");
                        }
                    }

                    element += 1;
                }

                Feature[] features2Array = features2.toArray(new Feature[0]);

                System.out.println("dim = " + features2Array.length);

                Feature feature = new Feature("row" + row, Type.VECTOR, new Value(features2Array));

                features.add(feature);

                row += 1;
            }

            return features;
        } finally {
            reader.close();
        }
    }

    public static List<Feature> loadClimate() throws Exception {

        int NUM_ROWS = 120;
        int NUM_FEATURES = 7;

        File ifile = new File("climate_test_data.txt");
        FileInputStream fis = new FileInputStream(ifile);
        InputStreamReader is = new InputStreamReader(fis);
        BufferedReader reader = new BufferedReader(is);

        try {
            // [[0.0974102590765342,
            // 1.0874092678271159,
            // 1.0492666556516463,
            // 1.343108570920707,
            // 0.37333868077418936,
            // -0.9809142890482833,
            // -0.6151232800959352],

            List<Feature> features2 = new ArrayList<Feature>(NUM_ROWS);

            System.out.println("input data:");

            System.out.println("[");

            for (int row = 0; row < NUM_ROWS; row++) {

                double[] feature3Array = new double[NUM_FEATURES];

                for (int e = 0; e < NUM_FEATURES; e++) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }

                    String cleanLine = line.replace("[", "").replace("]", "")
                            .replace(",", "");

                    feature3Array[e] = Double.valueOf(cleanLine);
                }

                System.out.print("[");
                for (int e = 0; e < NUM_FEATURES; e++) {
                    System.out.print(feature3Array[e] + ",");
                }
                System.out.println("],");
                

                Feature feature3 = new Feature("row" + row, Type.VECTOR, new Value(feature3Array));
                features2.add(feature3);
            }

            System.out.println("]");

            System.out.println("- 30 -");

            Feature[] features2Array = features2.toArray(new Feature[0]);
            Feature feature = new Feature("item0", Type.VECTOR, new Value(features2Array));

            List<Feature> features = new LinkedList<Feature>();
            features.add(feature);

            return features;
        } finally {
            reader.close();
        }
    }
}
