package org.kie.trustyai.explainability.local.tssaliency;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.utils.models.TSSaliencyModel;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

            Map<String, Saliency> saliencyMap = saliencyResults.getSaliencies();
            Saliency saliency = saliencyMap.get("result");
            List<FeatureImportance> featureImportances = saliency.getPerFeatureImportance();
            FeatureImportance featureImportance = featureImportances.get(0);

            double[][] scoreResult = featureImportance.getScoreMatrix();

            for (int t = 0; t < 500; t++) {
                for (int f = 0; f < 1; f++) {
                    double score = scoreResult[t][f];
                    System.out.println(score);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testData() {
        try {
            List<Feature> data = load();

            IntStream.range(0, data.size()).forEach(fn -> {
                Feature feature = data.get(fn);
                System.out.println("\nName: " + feature.getName());
                List<Feature> values = Arrays.asList((Feature[]) feature.getValue().getUnderlyingObject());
                IntStream.range(0, values.size()).forEach(i -> {
                    System.out.print("f" + fn + "-" + i + ": " + Arrays.toString(values.get(i).getValue().asVector()) + ", ");
                });
            });

            List<Double> firstTwoLoad = new ArrayList<>();
            IntStream.range(0, data.size()).forEach(fn -> {
                if (fn < 2) {
                    Feature feature = data.get(fn);
                    List<Feature> values = Arrays.asList((Feature[]) feature.getValue().getUnderlyingObject());
                    IntStream.range(0, values.size()).forEach(i -> {
                        double[] v = values.get(i).getValue().asVector();
                        firstTwoLoad.add(v[0]);
                    });
                }
            });

            List<PredictionInput> inputs = new ArrayList<>(data.size());

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

            IntStream.range(0, inputs.size()).forEach(i -> {
                PredictionInput input = inputs.get(i);
                System.out.println("\nInput: " + i);
                IntStream.range(0, input.getFeatures().size()).forEach(j -> {
                    System.out.print("f" + i + "-" + j + ": " + Arrays.toString(input.getFeatures().get(j).getValue().asVector()) + ", ");
                });
            });

            List<Double> inputsLoad = new ArrayList<>();
            IntStream.range(0, inputs.size()).forEach(fn -> {
                if (fn < 2) {
                    List<Feature> fs = inputs.get(fn).getFeatures();
                    IntStream.range(0, fs.size()).forEach(i -> {
                        double[] v = fs.get(i).getValue().asVector();
                        inputsLoad.add(v[0]);
                    });
                }
            });

            System.out.println("Compare  ============================================================");

            IntStream.range(0, firstTwoLoad.size()).forEach(i -> {
                System.out.println(firstTwoLoad.get(i) + " - " + inputsLoad.get(i) + " = " + (firstTwoLoad.get(i) - inputsLoad.get(i)));
            });

            List<List<Double>> inputsJSON = new ArrayList<>();
            IntStream.range(0, inputs.size()).forEach(fn -> {
                if (fn < 2) {
                    List<Double> store = new ArrayList<>();
                    List<Feature> fs = inputs.get(fn).getFeatures();
                    IntStream.range(0, fs.size()).forEach(i -> {
                        double[] v = fs.get(i).getValue().asVector();
                        store.add(v[0]);
                    });
                    inputsJSON.add(store);
                }
            });

            // Write as JSON
            Map<String, List<Double>> map = new HashMap<>();
            map.put("f1", inputsJSON.get(0));
            map.put("f2", inputsJSON.get(1));

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String jsonString = objectMapper.writeValueAsString(map);
                System.out.println(jsonString);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void test2() {
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

            TSSaliencyExplainerV2 explainer = new TSSaliencyExplainerV2(new double[0], 50, 1000, 0);

            CompletableFuture<SaliencyResults> saliencyResultsCompletable = explainer.explainAsync(prediction, model,
                    null);
            SaliencyResults saliencyResults = saliencyResultsCompletable.get();

            Map<String, Saliency> saliencyMap = saliencyResults.getSaliencies();
            Saliency saliency = saliencyMap.get("result");
            List<FeatureImportance> featureImportances = saliency.getPerFeatureImportance();
            FeatureImportance featureImportance = featureImportances.get(0);

            double[][] scoreResult = featureImportance.getScoreMatrix();

            for (int t = 0; t < 500; t++) {
                for (int f = 0; f < 1; f++) {
                    double score = scoreResult[t][f];
                    System.out.println(score);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public List<Feature> load() throws Exception {

        InputStream is = getClass().getClassLoader().getResourceAsStream("ford/FordA_TEST.txt");
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader reader = new BufferedReader(isr);

        try {
            List<Feature> features = new LinkedList<>();

            int row = 0;
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }

                StringTokenizer st = new StringTokenizer(line, "\t");

                List<Feature> features2 = new LinkedList<>();

                int element = 0;
                while (st.hasMoreElements()) {
                    String value = (String) st.nextElement();
                    if (element > 0) {
                        double[] feature3Array = new double[1];
                        feature3Array[0] = Double.valueOf(value);
                        Feature feature3 = new Feature("element" + element, Type.VECTOR, new Value(feature3Array));
                        features2.add(feature3);

                        //                        if (row == 0) {
                        //                            System.out.print(value + ",");
                        //                        }
                    }

                    element += 1;
                }

                Feature[] features2Array = features2.toArray(new Feature[0]);

                //                System.out.println("dim = " + features2Array.length);

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
