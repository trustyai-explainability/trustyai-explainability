package org.kie.trustyai.explainability.local.tssaliency;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

import java.util.UUID;

public class TSSaliencyExplainerTest {

    @Test
    public void test1() {
        try {
            List<Feature> data = load();

            List<PredictionInput> inputs = new LinkedList<PredictionInput>();

            for (Feature datum : data) {

                // System.out.println("name: " + datum.getName());
                assert datum.getType() == Type.VECTOR;

                Feature[] features2 = (Feature[]) datum.getValue().getUnderlyingObject();

                List<Feature> features2List = Arrays.asList(features2);

                PredictionInput input = new PredictionInput(features2List);
                inputs.add(input);

                break;
            }

            // System.out.println("inputs = " + inputs);

            PredictionProvider model = new TSSaliencyModel();

            CompletableFuture<List<PredictionOutput>> result = model.predictAsync(inputs);
            List<PredictionOutput> results = result.get();

            for (PredictionOutput prediction : results) {
                List<Output> outList = prediction.getOutputs();

                for (Output output : outList) {
                    System.out.println(output);
                }
            }



            // public SimplePrediction(PredictionInput input, PredictionOutput output) {
            //     super(input, output);
            // }

            // public interface Prediction {

            //     PredictionInput getInput();
            
            //     PredictionOutput getOutput();
            
            //     UUID getExecutionId();
            // }

            PredictionInput predictionInput = inputs.get(0);

            // public PredictionOutput(List<Output> outputs) {
            //     this.outputs = outputs;
            // }

            PredictionOutput predictionOutput = results.get(0);

            UUID uuid = UUID.randomUUID();
            
            Prediction prediction = new SimplePrediction(predictionInput, predictionOutput, uuid);

            // public TSSaliencyExplainer(float[] baseValue, int gradientSamples, int steps, int randomSeed) {

            //     Giridhar Ganapavarapu
            //     3:02 PM
            //   these are two numbers.. based on these numbers, we generate those many samples around X
            //   3:02
            //   We can set default values
            //   3:03
            //   like ng = 100 and nalpha = 10

           
            TSSaliencyExplainer explainer = new TSSaliencyExplainer(new double[0], 100, 10, 0);

            // public CompletableFuture<IntegratedGradient> explainAsync(Prediction prediction, PredictionProvider model,
            // Consumer<IntegratedGradient> intermediateResultsConsumer) {

   

    }catch(

    Exception e)
    {
        e.printStackTrace();
    }

    }

    public List<Feature> load() throws Exception {

        File ifile = new File("../ford/FordA_TRAIN.txt");
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
                    }

                    element += 1;
                }

                Feature[] features2Array = features2.toArray(new Feature[0]);
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
