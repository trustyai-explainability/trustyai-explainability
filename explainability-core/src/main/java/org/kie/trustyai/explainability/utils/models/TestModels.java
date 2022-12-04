/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.trustyai.explainability.utils.models;

import java.util.*;

import org.kie.trustyai.explainability.model.*;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class TestModels {

    public static PredictionProvider getFeaturePassModel(int featureIndex) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                Feature feature = features.get(featureIndex);
                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(new Output("feature-" + featureIndex, feature.getType(), feature.getValue(),
                                1d)));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getSumSkipModel(int skipFeatureIndex) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (int i = 0; i < features.size(); i++) {
                    if (skipFeatureIndex != i) {
                        result += features.get(i).getValue().asNumber();
                    }
                }
                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(new Output("sum-but" + skipFeatureIndex, Type.NUMBER, new Value(result), 1d)));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getNoisySumModel(SplittableRandom rn, double noiseMagnitude, long noiseSamples) {
        return inputs -> supplyAsync(() -> {
            Iterator<Double> noiseStream = rn.doubles(noiseSamples).iterator();
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (int i = 0; i < features.size(); i++) {
                    result += features.get(i).getValue().asNumber() + ((noiseStream.next() - .5) * noiseMagnitude);
                }
                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(new Output("noisy-sum", Type.NUMBER, new Value(result), 1d)));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getLinearModel(double[] weights) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (int i = 0; i < features.size(); i++) {
                    result += features.get(i).getValue().asNumber() * weights[i];
                }
                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(new Output("linear-sum", Type.NUMBER, new Value(result), 1d)));

                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getLinearThresholdModel(double[] weights, double threshold) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (int i = 0; i < features.size(); i++) {
                    result += features.get(i).getValue().asNumber() * weights[i];
                }
                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(new Output("linear-sum-above-thresh", Type.BOOLEAN, new Value(result > threshold), 1d)));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getSumSkipTwoOutputModel(int skipFeatureIndex) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (int i = 0; i < features.size(); i++) {
                    if (skipFeatureIndex != i) {
                        result += features.get(i).getValue().asNumber();
                    }
                }
                Output output0 = new Output("sum-but" + skipFeatureIndex, Type.NUMBER, new Value(result), 1d);
                Output output1 = new Output("sum-but" + skipFeatureIndex + "*2", Type.NUMBER, new Value(result * 2), 1d);

                PredictionOutput predictionOutput = new PredictionOutput(List.of(output0, output1));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getTwoOutputSemiCategoricalModel(int categoricalIndex) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (int i = 0; i < features.size(); i++) {
                    if (categoricalIndex == i) {
                        result += features.get(i).getValue().equals("A") ? 10. : -10;
                    } else {
                        result += features.get(i).getValue().asNumber();
                    }
                }
                Output output0 = new Output("Semi-Categorical", Type.NUMBER, new Value(result), 1d);
                Output output1 = new Output("Semi-Categorical*2", Type.NUMBER, new Value(result * 2), 1d);

                PredictionOutput predictionOutput = new PredictionOutput(List.of(output0, output1));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    /**
     * Test model which returns the inputs as outputs, except for a single specified feature
     *
     * @param featureIndex Index of the input feature to omit from output
     * @return A {@link PredictionProvider} model
     */
    public static PredictionProvider getFeatureSkipModel(int featureIndex) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                List<Output> outputs = new ArrayList<>();
                for (int i = 0; i < features.size(); i++) {
                    if (i != featureIndex) {
                        Feature feature = features.get(i);
                        outputs.add(new Output(feature.getName(), feature.getType(), feature.getValue(), 1.0));
                    }
                }
                PredictionOutput predictionOutput = new PredictionOutput(outputs);
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getEvenSumModel(int skipFeatureIndex) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (int i = 0; i < features.size(); i++) {
                    if (skipFeatureIndex != i) {
                        result += features.get(i).getValue().asNumber();
                    }
                }
                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(new Output("sum-even-but" + skipFeatureIndex, Type.BOOLEAN, new Value(((int) result) % 2 == 0),
                                1d)));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getSumThresholdModel(double center, double epsilon) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (Feature feature : features) {
                    result += feature.getValue().asNumber();
                }
                final boolean inside = (result >= center - epsilon && result <= center + epsilon);
                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(new Output("inside", Type.BOOLEAN, new Value(inside), Math.abs(epsilon - Math.abs((result - center))))));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getSumThresholdDifferentiableModel(double center, double epsilon) {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                double result = 0;
                for (Feature feature : features) {
                    result += feature.getValue().asNumber();
                }
                final boolean inside = (result >= center - epsilon && result <= center + epsilon);
                double distance_from_center = Math.abs(result - center);
                double value = inside ? 0 : distance_from_center;
                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(
                                new Output("inside", Type.BOOLEAN, new Value(inside), 1.0),
                                new Output("distance", Type.NUMBER, new Value(value), 1.0)));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getDummyTextClassifier() {
        List<String> blackList = Arrays.asList("money", "$", "£", "bitcoin");
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> outputs = new LinkedList<>();
            for (PredictionInput input : inputs) {
                boolean spam = false;
                for (Feature f : input.getFeatures()) {
                    if (!spam) {
                        String s = f.getValue().asString();
                        String[] words = s.split(" ");
                        for (String w : words) {
                            if (blackList.contains(w)) {
                                spam = true;
                                break;
                            }
                        }
                    }
                }
                Output output = new Output("spam", Type.BOOLEAN, new Value(spam), 1d);
                outputs.add(new PredictionOutput(List.of(output)));
            }
            return outputs;
        });
    }

    public static PredictionProvider getCategoricalRegressor() {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> outputs = new LinkedList<>();
            for (PredictionInput input : inputs) {
                int calories = 0;
                int fruitsEaten = 0;
                for (Feature f : input.getFeatures()) {
                    if (!f.getType().equals(Type.CATEGORICAL)) {
                        throw new IllegalArgumentException("Prediction Inputs are not categorical");
                    }
                    switch (f.getValue().asString()) {
                        case "avocado":
                            calories += 322;
                            fruitsEaten += 1;
                            break;
                        case "banana":
                            calories += 105;
                            fruitsEaten += 1;
                            break;
                        case "carrot":
                            calories += 25;
                            fruitsEaten += 1;
                            break;
                        case "dragonfruit":
                            calories += 61;
                            fruitsEaten += 1;
                            break;
                        default:
                            break;
                    }
                }
                Output outputCal = new Output("calories", Type.NUMBER, new Value(calories), 1d);
                Output outputFE = new Output("fruit_eaten", Type.NUMBER, new Value(fruitsEaten), 1d);
                outputs.add(new PredictionOutput(List.of(outputCal, outputFE)));
            }
            return outputs;
        });
    }

    public static PredictionProvider getSymbolicArithmeticModel() {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> predictionOutputs = new LinkedList<>();
            final String OPERAND_FEATURE_NAME = "operand";
            for (PredictionInput predictionInput : inputs) {
                List<Feature> features = predictionInput.getFeatures();
                // Find a valid operand feature, if any
                Optional<String> operand = features.stream().filter(f -> OPERAND_FEATURE_NAME.equals(f.getName()))
                        .map(f -> f.getValue().asString()).findFirst();
                if (!operand.isPresent()) {
                    throw new IllegalArgumentException("No valid operand found in features");
                }
                final String operandValue = operand.get();
                double result = 0;
                // Apply the found operand to the rest of the features
                for (Feature feature : features) {
                    if (!OPERAND_FEATURE_NAME.equals(feature.getName())) {
                        switch (operandValue) {
                            case "+":
                                result += feature.getValue().asNumber();
                                break;
                            case "-":
                                result -= feature.getValue().asNumber();
                                break;
                            case "*":
                                result *= feature.getValue().asNumber();
                                break;
                            case "/":
                                result /= feature.getValue().asNumber();
                                break;
                        }
                    }
                }

                PredictionOutput predictionOutput = new PredictionOutput(
                        List.of(new Output("result", Type.NUMBER, new Value(result), 1d)));
                predictionOutputs.add(predictionOutput);
            }
            return predictionOutputs;
        });
    }

    public static PredictionProvider getFixedOutputClassifier() {
        return inputs -> supplyAsync(() -> {
            List<PredictionOutput> outputs = new LinkedList<>();
            for (PredictionInput ignored : inputs) {
                Output output = new Output("class", Type.BOOLEAN, new Value(false), 1d);
                outputs.add(new PredictionOutput(List.of(output)));
            }
            return outputs;
        });
    }

    public static PredictionProvider getSimpleBiasedClassifier(int biasedFeature,
            Value biasedValue,
            double threshold) {
        final Random random = new Random();
        return inputs -> supplyAsync(() -> {
            final List<PredictionOutput> outputs = new ArrayList<>();
            for (PredictionInput input : inputs) {

                boolean approved;
                if (input.getFeatures().get(biasedFeature).getValue().equals(biasedValue)) {
                    approved = random.nextDouble() < threshold;
                } else {
                    approved = random.nextDouble() < 0.5;
                }
                final Output output = new Output("approved", Type.BOOLEAN, new Value(approved), 1.0);
                outputs.add(new PredictionOutput(List.of(output)));
            }
            return outputs;
        });
    }

}
