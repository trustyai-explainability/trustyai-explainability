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
package org.kie.trustyai.metrics.fairness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.metrics.accuracy.ConfusionMatrix;

public class FairnessMetricsUtils {

    private FairnessMetricsUtils() {
    }

    public static double getFavorableLabelProbability(Predicate<PredictionInput> groupSelector, List<PredictionInput> samples,
            PredictionProvider model, Output favorableOutput) throws ExecutionException, InterruptedException {
        String outputName = favorableOutput.getName();
        Value outputValue = favorableOutput.getValue();

        List<PredictionOutput> selectedOutputs = getSelectedPredictionOutputs(groupSelector, samples, model);

        double numSelected = selectedOutputs.size();
        long numFavorableSelected = selectedOutputs.stream().map(po -> po.getByName(outputName)).map(Optional::get)
                .filter(o -> o.getValue().equals(outputValue)).count();

        return numFavorableSelected / numSelected;
    }

    private static List<PredictionOutput> getSelectedPredictionOutputs(Predicate<PredictionInput> groupSelector, List<PredictionInput> samples, PredictionProvider model)
            throws InterruptedException, ExecutionException {
        List<PredictionInput> selected = samples.stream().filter(groupSelector).collect(Collectors.toList());

        return model.predictAsync(selected).get();
    }

    public static int countTruePositives(List<List<Value>> testOutputs, List<List<Value>> truthOutputs, Predicate<List<Value>> positive) {
        final int N = testOutputs.size();
        final IntPredicate predictionMatches = i -> testOutputs.get(i).equals(truthOutputs.get(i));
        final IntPredicate testRowIsPositive = row -> positive.test(truthOutputs.get(row));
        return (int) IntStream.range(0, N).parallel().filter(predictionMatches).filter(testRowIsPositive).count();
    }

    public static int countTrueNegatives(List<List<Value>> testOutputs, List<List<Value>> truthOutputs, Predicate<List<Value>> positive) {
        final int N = testOutputs.size();
        final IntPredicate predictionMatches = i -> testOutputs.get(i).equals(truthOutputs.get(i));
        final IntPredicate testRowIsPositive = row -> positive.test(truthOutputs.get(row));
        return (int) IntStream.range(0, N).parallel().filter(predictionMatches).filter(testRowIsPositive.negate()).count();
    }

    public static int countFalsePositives(List<List<Value>> testOutputs, List<List<Value>> truthOutputs, Predicate<List<Value>> positive) {
        final int N = testOutputs.size();
        final IntPredicate predictionMatches = i -> testOutputs.get(i).equals(truthOutputs.get(i));
        final IntPredicate testRowIsPositive = row -> positive.test(truthOutputs.get(row));
        return (int) IntStream.range(0, N).parallel().filter(testRowIsPositive).filter(predictionMatches.negate()).count();
    }

    public static int countFalseNegatives(List<List<Value>> testOutputs, List<List<Value>> truthOutputs, Predicate<List<Value>> positive) {
        final int N = testOutputs.size();
        final IntPredicate predictionMatches = i -> testOutputs.get(i).equals(truthOutputs.get(i));
        final IntPredicate testRowIsPositive = row -> positive.test(truthOutputs.get(row));
        return (int) IntStream.range(0, N).parallel().filter(testRowIsPositive.negate()).filter(predictionMatches.negate()).count();
    }

    public static ConfusionMatrix calculateConfusionMatrix(Dataframe test, Dataframe truth, Predicate<List<Value>> positive) {

        final List<List<Value>> testOutputs = test.getOutputRows();
        final List<List<Value>> truthOutputs = truth.getOutputRows();

        return ConfusionMatrix.create(
                countTruePositives(testOutputs, truthOutputs, positive),
                countTrueNegatives(testOutputs, truthOutputs, positive),
                countFalsePositives(testOutputs, truthOutputs, positive),
                countFalseNegatives(testOutputs, truthOutputs, positive));
    }

    /**
     * Count true / false favorable and true / false unfavorable outputs with respect to a specified output selector.
     * 
     * @param dataset dataset used to match predictions with labels
     * @param predictionOutputs predictions to match with the dataset labels
     * @param outputSelector selector to define positive labelled samples / predictions
     * @return a map containing counts for true positives ("tp"), true negatives ("tn"), false positives ("fp"), false negatives ("fn")
     */
    public static Map<String, Integer> countMatchingOutputSelector(Dataset dataset, List<PredictionOutput> predictionOutputs,
            Predicate<PredictionOutput> outputSelector) {
        assert predictionOutputs.size() == dataset.getData().size() : "dataset and predictions must have same size";
        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;
        int i = 0;
        for (Prediction trainingExample : dataset.getData()) {
            if (outputSelector.test(trainingExample.getOutput())) {
                // positive
                if (outputSelector.test(predictionOutputs.get(i))) {
                    tp++;
                } else {
                    fn++;
                }
            } else {
                // negative
                if (outputSelector.test(predictionOutputs.get(i))) {
                    fp++;
                } else {
                    tn++;
                }
            }
            i++;
        }
        Map<String, Integer> map = new HashMap<>();
        map.put("tp", tp);
        map.put("tn", tn);
        map.put("fp", fp);
        map.put("fn", fn);
        return map;
    }

}
