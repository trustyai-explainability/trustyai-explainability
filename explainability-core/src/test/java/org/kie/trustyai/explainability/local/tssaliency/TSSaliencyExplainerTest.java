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

                // System.out.println("name: " + datum.getName());
                assert datum.getType() == Type.VECTOR;

                Feature[] features2 = (Feature[]) datum.getValue().getUnderlyingObject();

                List<Feature> features2List = Arrays.asList(features2);

                // System.out.println(features2List);

                PredictionInput input = new PredictionInput(features2List);
                inputs.add(input);

                if (++count == 2) {
                    break;
                }
            }

            // System.out.println("inputs = " + inputs);

            PredictionProvider model = new TSSaliencyModel();

            CompletableFuture<List<PredictionOutput>> result = model.predictAsync(inputs);
            List<PredictionOutput> results = result.get();

            for (PredictionOutput prediction : results) {
                List<Output> outList = prediction.getOutputs();

                for (Output output : outList) {
                    System.out.println("*********** " + output);
                }
            }

            // System.exit(1);

            // ******** NOTE need a loop for e.g., Ford data

            PredictionInput predictionInput = inputs.get(0);

            PredictionOutput predictionOutput = results.get(0);

            UUID uuid = UUID.randomUUID();

            Prediction prediction = new SimplePrediction(predictionInput, predictionOutput, uuid);

            // Giridhar Ganapavarapu
            // 3:02 PM
            // these are two numbers.. based on these numbers, we generate those many
            // samples around X
            // 3:02
            // We can set default values
            // 3:03
            // like ng = 100 and nalpha = 10

            TSSaliencyExplainer explainer = new TSSaliencyExplainer(new double[0], 50, 1000, 0);

            CompletableFuture<SaliencyResults> saliencyResultsCompletable = explainer.explainAsync(prediction, model,
                    null);
            SaliencyResults saliencyResults = saliencyResultsCompletable.get();

            // System.out.println(saliencyResults.getSaliencies());

            Map<String, Saliency> saliencyMap = saliencyResults.getSaliencies();

            for (int t = 0; t < 500; t++) {
                for (int f = 0; f < 1; f++) {
                    String name = "IG[" + t + "][" + f + "]";
                    Saliency saliency = saliencyMap.get(name);
                    Output output = saliency.getOutput();
                    double score = output.getScore();
                    System.out.println(score);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

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

}
