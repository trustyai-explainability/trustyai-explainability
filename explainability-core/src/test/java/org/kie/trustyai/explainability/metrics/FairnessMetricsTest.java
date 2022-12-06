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
package org.kie.trustyai.explainability.metrics;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.utils.DataUtils;
import org.kie.trustyai.explainability.utils.models.TestModels;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class FairnessMetricsTest {

    @Test
    void testIndividualConsistencyTextClassifier() throws ExecutionException, InterruptedException {
        BiFunction<PredictionInput, List<PredictionInput>, List<PredictionInput>> proximityFunction = (predictionInput, predictionInputs) -> {
            String reference = DataUtils.textify(predictionInput);
            return predictionInputs.stream().sorted(
                    (o1, o2) -> (StringUtils.getFuzzyDistance(DataUtils.textify(o2), reference, Locale.getDefault())
                            - StringUtils.getFuzzyDistance(DataUtils.textify(o1), reference, Locale.getDefault())))
                    .collect(Collectors.toList()).subList(1, 3);
        };
        List<PredictionInput> testInputs = getTestInputs();
        PredictionProvider model = TestModels.getDummyTextClassifier();
        double individualConsistency = FairnessMetrics.individualConsistency(proximityFunction, testInputs, model);
        assertThat(individualConsistency).isBetween(0d, 1d);
    }

    @Test
    void testGroupSPDTextClassifier() throws ExecutionException, InterruptedException {
        List<PredictionInput> testInputs = getTestInputs();
        PredictionProvider model = TestModels.getDummyTextClassifier();
        Predicate<PredictionInput> selector = predictionInput -> DataUtils.textify(predictionInput).contains("please");
        Output output = new Output("spam", Type.BOOLEAN, new Value(false), 1.0);
        double spd = FairnessMetrics.groupStatisticalParityDifference(selector, testInputs, model, output);
        assertThat(spd).isBetween(0.1, 0.2);
    }

    @Test
    void testGroupSPDTextClassifierDataframe() throws ExecutionException, InterruptedException {
        final List<PredictionInput> testInputs = getTestInputs();
        final PredictionProvider model = TestModels.getDummyTextClassifier();
        final List<PredictionOutput> testOutputs = model.predictAsync(testInputs).get();

        final Dataframe dataframe = Dataframe.createFrom(testInputs, testOutputs);

        final Predicate<List<Value>> priviledgedFilter = values -> values.stream().anyMatch(v -> v.asString().contains("please"));
        final Dataframe priviledged = dataframe.filterRowsByInputs(priviledgedFilter);
        final Dataframe unpriviledged = dataframe.filterRowsByInputs(priviledgedFilter.negate());

        final Output output = new Output("spam", Type.BOOLEAN, new Value(false), 1.0);
        double spd = FairnessMetrics.groupStatisticalParityDifference(priviledged, unpriviledged, List.of(output));
        assertThat(spd).isBetween(0.1, 0.2);
    }

    @Test
    void testGroupDIRTextClassifier() throws ExecutionException, InterruptedException {
        List<PredictionInput> testInputs = getTestInputs();
        PredictionProvider model = TestModels.getDummyTextClassifier();
        Predicate<PredictionInput> selector = predictionInput -> DataUtils.textify(predictionInput).contains("please");
        Output output = new Output("spam", Type.BOOLEAN, new Value(false), 1.0);
        double dir = FairnessMetrics.groupDisparateImpactRatio(selector, testInputs, model, output);
        assertThat(dir).isPositive();
    }

    @Test
    void testGroupDIRTextClassifierDataframe() throws ExecutionException, InterruptedException {
        final List<PredictionInput> testInputs = getTestInputs();
        final PredictionProvider model = TestModels.getDummyTextClassifier();
        final List<PredictionOutput> testOutputs = model.predictAsync(testInputs).get();

        final Dataframe dataframe = Dataframe.createFrom(testInputs, testOutputs);

        final Predicate<List<Value>> priviledgedFilter = values -> values.stream().anyMatch(v -> v.asString().contains("please"));
        final Dataframe priviledged = dataframe.filterRowsByInputs(priviledgedFilter);
        final Dataframe unpriviledged = dataframe.filterRowsByInputs(priviledgedFilter.negate());

        final Output output = new Output("spam", Type.BOOLEAN, new Value(false), 1.0);
        double dir = FairnessMetrics.groupDisparateImpactRatio(priviledged, unpriviledged, List.of(output));
        assertThat(dir).isBetween(1.0, 2.0);
    }

    @Test
    void testGroupAODTextClassifier() throws ExecutionException, InterruptedException {
        List<Prediction> predictions = getTestData();
        Dataset dataset = new Dataset(predictions);
        PredictionProvider model = TestModels.getDummyTextClassifier();
        Predicate<PredictionInput> inputSelector = predictionInput -> DataUtils.textify(predictionInput).contains("please");
        Predicate<PredictionOutput> outputSelector = predictionOutput -> predictionOutput.getByName("spam").get().getValue().asNumber() == 0;
        double aod = FairnessMetrics.groupAverageOddsDifference(inputSelector, outputSelector, dataset, model);
        assertThat(aod).isBetween(-1d, 1d);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void testGroupAODBiasedClassifier(int seed) throws ExecutionException, InterruptedException {
        final Random random = new Random(seed);
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < 5000; i++) {
            final List<Feature> features = new ArrayList<>();
            features.add(FeatureFactory.newNumericalFeature("age", 18 + random.nextInt(50)));
            features.add(FeatureFactory.newNumericalFeature("income", 10000 + random.nextInt(90000)));
            features.add(FeatureFactory.newCategoricalFeature("gender", random.nextBoolean() ? "M" : "F"));
            final PredictionInput input = new PredictionInput(features);
            final Value approved = new Value(random.nextBoolean());
            final PredictionOutput output = new PredictionOutput(List.of(new Output("approved", Type.BOOLEAN, approved, 1.0)));
            predictions.add(new SimplePrediction(input, output));

        }

        Dataset dataset = new Dataset(predictions);
        PredictionProvider model = TestModels.getSimpleBiasedClassifier(2, new Value("M"), 0.75);
        Predicate<PredictionInput> inputSelector = predictionInput -> predictionInput.getFeatures().get(2).getValue().asString().equals("M");
        Predicate<PredictionOutput> outputSelector = predictionOutput -> (boolean) predictionOutput.getOutputs().get(0).getValue().getUnderlyingObject();
        double aod = FairnessMetrics.groupAverageOddsDifference(inputSelector, outputSelector, dataset, model);
        System.out.println(aod);
        assertThat(aod).isBetween(-0.3, -0.1);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void testGroupAODTextClassifierDataframe(int seed) throws ExecutionException, InterruptedException {
        final Random random = new Random(seed);
        final List<PredictionInput> inputs = new ArrayList<>();
        final List<PredictionOutput> outputs = new ArrayList<>();

        for (int i = 0; i < 5000; i++) {
            final List<Feature> features = new ArrayList<>();
            features.add(FeatureFactory.newNumericalFeature("age", 18 + random.nextInt(50)));
            features.add(FeatureFactory.newNumericalFeature("income", 10000 + random.nextInt(90000)));
            features.add(FeatureFactory.newCategoricalFeature("gender", random.nextBoolean() ? "M" : "F"));
            final PredictionInput input = new PredictionInput(features);
            inputs.add(input);
            final Value approved = new Value(random.nextBoolean());
            final PredictionOutput output = new PredictionOutput(List.of(new Output("approved", Type.BOOLEAN, approved, 1.0)));
            outputs.add(output);
        }

        final Dataframe test = Dataframe.createFrom(inputs, outputs);
        PredictionProvider model = TestModels.getSimpleBiasedClassifier(2, new Value("M"), 0.75);
        final List<PredictionOutput> predictedOutputs = model.predictAsync(inputs).get();
        final Dataframe truth = Dataframe.createFrom(inputs, predictedOutputs);
        double aod = FairnessMetrics.groupAverageOddsDifference(test, truth, List.of(2), List.of(new Value("M")), List.of(new Value(true)));
        System.out.println(aod);
        assertThat(aod).isBetween(-0.3, -0.1);
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2 })
    void testGroupAPVBiasClassifierDataframe(int seed) throws ExecutionException, InterruptedException {
        final Random random = new Random(seed);
        final List<PredictionInput> inputs = new ArrayList<>();
        final List<PredictionOutput> outputs = new ArrayList<>();

        for (int i = 0; i < 5000; i++) {
            final List<Feature> features = new ArrayList<>();
            features.add(FeatureFactory.newNumericalFeature("age", 18 + random.nextInt(50)));
            features.add(FeatureFactory.newNumericalFeature("income", 10000 + random.nextInt(90000)));
            features.add(FeatureFactory.newCategoricalFeature("gender", random.nextBoolean() ? "M" : "F"));
            final PredictionInput input = new PredictionInput(features);
            inputs.add(input);
            final Value approved = new Value(random.nextBoolean());
            final PredictionOutput output = new PredictionOutput(List.of(new Output("approved", Type.BOOLEAN, approved, 1.0)));
            outputs.add(output);
        }

        final Dataframe test = Dataframe.createFrom(inputs, outputs);
        PredictionProvider model = TestModels.getSimpleBiasedClassifier(2, new Value("M"), 0.75);
        final List<PredictionOutput> predictedOutputs = model.predictAsync(inputs).get();
        final Dataframe truth = Dataframe.createFrom(inputs, predictedOutputs);
        double aod = FairnessMetrics.groupAveragePredictiveValueDifference(test, truth, List.of(2), List.of(new Value("M")), List.of(new Value(true)));
        System.out.println(aod);
        assertThat(aod).isBetween(-0.15, 0.15);
    }

    @Test
    void testGroupAPVDTextClassifier() throws ExecutionException, InterruptedException {
        List<Prediction> predictions = getTestData();
        Dataset dataset = new Dataset(predictions);
        PredictionProvider model = TestModels.getDummyTextClassifier();
        Predicate<PredictionInput> inputSelector = predictionInput -> DataUtils.textify(predictionInput).contains("please");
        Predicate<PredictionOutput> outputSelector = predictionOutput -> predictionOutput.getByName("spam").get().getValue().asNumber() == 0;
        double apvd = FairnessMetrics.groupAveragePredictiveValueDifference(inputSelector, outputSelector, dataset, model);
        assertThat(apvd).isBetween(-1d, 1d);
    }

    private List<PredictionInput> getTestInputs() {
        List<PredictionInput> inputs = new ArrayList<>();
        Function<String, List<String>> tokenizer = s -> Arrays.asList(s.split(" ").clone());
        List<Feature> features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "urgent inquiry", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "please give me some money", tokenizer));
        inputs.add(new PredictionInput(features));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "please reply", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "we got urgent matter! please reply", tokenizer));
        inputs.add(new PredictionInput(features));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "please reply", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "we got money matter! please reply", tokenizer));
        inputs.add(new PredictionInput(features));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "inquiry", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "would you like to get a 100% secure way to invest your money?", tokenizer));
        inputs.add(new PredictionInput(features));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "you win", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "you just won an incredible 1M $ prize !", tokenizer));
        inputs.add(new PredictionInput(features));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "prize waiting", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "you are the lucky winner of a 100k $ prize", tokenizer));
        inputs.add(new PredictionInput(features));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "urgent matter", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "we got an urgent inquiry for you to answer.", tokenizer));
        inputs.add(new PredictionInput(features));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "password change", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "you just requested to change your password", tokenizer));
        inputs.add(new PredictionInput(features));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "password stolen", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "we stole your password, if you want it back, send some money .", tokenizer));
        inputs.add(new PredictionInput(features));
        return inputs;
    }

    private List<Prediction> getTestData() {
        List<Prediction> data = new ArrayList<>();
        Function<String, List<String>> tokenizer = s -> Arrays.asList(s.split(" ").clone());
        List<Feature> features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "urgent inquiry", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "please give me some money", tokenizer));
        Output output = new Output("spam", Type.BOOLEAN, new Value(true), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "do not reply", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "if you asked to reset your password, ignore this", tokenizer));
        output = new Output("spam", Type.BOOLEAN, new Value(false), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "please reply", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "we got money matter! please reply", tokenizer));
        output = new Output("spam", Type.BOOLEAN, new Value(true), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "inquiry", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "would you like to get a 100% secure way to invest your money?", tokenizer));
        output = new Output("spam", Type.BOOLEAN, new Value(true), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "clear some space", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "you just finished your space, upgrade today for 1 $ a week", tokenizer));
        output = new Output("spam", Type.BOOLEAN, new Value(false), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "prize waiting", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "you are the lucky winner of a 100k $ prize", tokenizer));
        output = new Output("spam", Type.BOOLEAN, new Value(true), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "urgent matter", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "we got an urgent inquiry for you to answer.", tokenizer));
        output = new Output("spam", Type.BOOLEAN, new Value(true), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "password change", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "you just requested to change your password", tokenizer));
        output = new Output("spam", Type.BOOLEAN, new Value(false), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        features = new ArrayList<>();
        features.add(FeatureFactory.newFulltextFeature("subject", "password stolen", tokenizer));
        features.add(FeatureFactory.newFulltextFeature("text", "we stole your password, if you want it back, send some money .", tokenizer));
        output = new Output("spam", Type.BOOLEAN, new Value(true), 1);
        data.add(new SimplePrediction(new PredictionInput(features), new PredictionOutput(List.of(output))));
        return data;
    }
}
