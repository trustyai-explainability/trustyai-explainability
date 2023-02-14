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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.*;

public class FairnessMetrics {

    private FairnessMetrics() {
    }

    /**
     * Calculate individual fairness in terms of consistency of predictions across similar inputs.
     *
     * @param proximityFunction a function that finds the top k similar inputs, given a reference input and a list of inputs
     * @param samples a list of inputs to be tested for consistency
     * @param predictionProvider the model under inspection
     * @return the consistency measure
     * @throws ExecutionException if any error occurs during model prediction
     * @throws InterruptedException if timeout or other interruption issues occur during model prediction
     */
    public static double individualConsistency(BiFunction<PredictionInput, List<PredictionInput>, List<PredictionInput>> proximityFunction,
            List<PredictionInput> samples, PredictionProvider predictionProvider) throws ExecutionException, InterruptedException {
        double consistency = 1;
        for (PredictionInput input : samples) {
            List<PredictionOutput> predictionOutputs = predictionProvider.predictAsync(List.of(input)).get();
            PredictionOutput predictionOutput = predictionOutputs.get(0);
            List<PredictionInput> neighbors = proximityFunction.apply(input, samples);
            List<PredictionOutput> neighborsOutputs = predictionProvider.predictAsync(neighbors).get();
            for (Output output : predictionOutput.getOutputs()) {
                Value originalValue = output.getValue();
                for (PredictionOutput neighborOutput : neighborsOutputs) {
                    Output currentOutput = neighborOutput.getByName(output.getName()).orElse(null);
                    if (currentOutput != null && !originalValue.equals(currentOutput.getValue())) {
                        consistency -= 1f / (neighbors.size() * predictionOutput.getOutputs().size() * samples.size());
                    }
                }
            }
        }
        return consistency;
    }

    // ==== SPD ========================================================================================================
    /**
     * Calculate statistical/demographic parity difference (SPD).
     *
     * @param groupSelector a predicate used to select the privileged group
     * @param samples a list of inputs to be used for testing fairness
     * @param model the model to be tested for fairness
     * @param favorableOutput an output that is considered favorable / desirable
     * @return SPD, between 0 and 1
     * @throws ExecutionException if any error occurs during model prediction
     * @throws InterruptedException if timeout or other interruption issues occur during model prediction
     */
    public static double groupStatisticalParityDifference(Predicate<PredictionInput> groupSelector, List<PredictionInput> samples,
            PredictionProvider model, Output favorableOutput)
            throws ExecutionException, InterruptedException {

        double probabilityUnprivileged = getFavorableLabelProbability(groupSelector.negate(), samples, model, favorableOutput);
        double probabilityPrivileged = getFavorableLabelProbability(groupSelector, samples, model, favorableOutput);

        return probabilityUnprivileged - probabilityPrivileged;
    }

    /**
     * Calculate statistical/demographic parity difference (SPD)
     *
     * @param samples A dataframe of inputs to be used for testing fairness
     * @param model A model as a {@link PredictionProvider} to be tested for fairness
     * @param priviledgeColumns A {@link List} of integers specifying the privileged columns
     * @param priviledgeValues A {@link List} of {@link Value} specifying the privileged values
     * @param favorableOutput The favorable output
     * @return The group SPD score
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static double groupStatisticalParityDifference(Dataframe samples, PredictionProvider model, List<Integer> priviledgeColumns,
            List<Value> priviledgeValues, List<Output> favorableOutput) throws ExecutionException, InterruptedException {

        final List<PredictionInput> inputs = samples.asPredictionInputs();
        final List<PredictionOutput> outputs = model.predictAsync(inputs).get();

        final Dataframe data = Dataframe.createFrom(inputs, outputs);

        final Predicate<List<Value>> priviledgeFilter = values -> priviledgeColumns
                .stream().map(values::get)
                .collect(Collectors.toList())
                .equals(priviledgeValues);

        final Dataframe privileged = data.filterRowsByInputs(priviledgeFilter);
        final Dataframe unprivileged = data.filterRowsByInputs(priviledgeFilter.negate());

        return groupStatisticalParityDifference(privileged, unprivileged, favorableOutput);
    }

    /**
     * Calculate statistical/demographic parity difference (SPD) when the
     * labels are pre-calculated.
     *
     * @param priviledged {@link Dataframe} with the priviledged groups
     * @param unpriviledged {@link Dataframe} with the unpriviledged groups
     * @param favorableOutput an output that is considered favorable / desirable
     * @return SPD, between 0 and 1
     */
    public static double groupStatisticalParityDifference(Dataframe priviledged, Dataframe unpriviledged, List<Output> favorableOutput) {

        final List<Value> favorableValues = favorableOutput.stream().map(Output::getValue).collect(Collectors.toUnmodifiableList());
        final Predicate<List<Value>> match = values -> values.equals(favorableValues);

        final double probabilityPrivileged = (double) priviledged.filterRowsByOutputs(match).getRowDimension() / (double) priviledged.getRowDimension();
        final double probabilityUnprivileged = (double) unpriviledged.filterRowsByOutputs(match).getRowDimension() / (double) unpriviledged.getRowDimension();

        return probabilityUnprivileged - probabilityPrivileged;
    }

    // === DIR =========================================================================================================
    /**
     * Calculate disparate impact ratio (DIR).
     *
     * @param groupSelector a predicate used to select the privileged group
     * @param samples a list of inputs to be used for testing fairness
     * @param model the model to be tested for fairness
     * @param favorableOutput an output that is considered favorable / desirable
     * @return SPD, between 0 and 1
     * @throws ExecutionException if any error occurs during model prediction
     * @throws InterruptedException if timeout or other interruption issues occur during model prediction
     */
    public static double groupDisparateImpactRatio(Predicate<PredictionInput> groupSelector, List<PredictionInput> samples,
            PredictionProvider model, Output favorableOutput)
            throws ExecutionException, InterruptedException {

        double probabilityUnprivileged = getFavorableLabelProbability(groupSelector.negate(), samples, model, favorableOutput);
        double probabilityPrivileged = getFavorableLabelProbability(groupSelector, samples, model, favorableOutput);

        return probabilityUnprivileged / probabilityPrivileged;
    }

    /**
     * Calculate disparate impact ratio (DIR).
     *
     * @param samples A dataframe of inputs to be used for testing fairness
     * @param model A model as a {@link PredictionProvider} to be tested for fairness
     * @param priviledgeColumns A {@link List} of integers specifying the privileged columns
     * @param priviledgeValues A {@link List} of {@link Value} specifying the privileged values
     * @param favorableOutput The favorable output
     * @return The DIR score
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static double groupDisparateImpactRatio(Dataframe samples, PredictionProvider model, List<Integer> priviledgeColumns,
            List<Value> priviledgeValues, List<Output> favorableOutput) throws ExecutionException, InterruptedException {

        final List<PredictionInput> inputs = samples.asPredictionInputs();
        final List<PredictionOutput> outputs = model.predictAsync(inputs).get();

        final Dataframe data = Dataframe.createFrom(inputs, outputs);

        final Predicate<List<Value>> priviledgeFilter = values -> priviledgeColumns
                .stream().map(values::get)
                .collect(Collectors.toList())
                .equals(priviledgeValues);

        final Dataframe privileged = data.filterRowsByInputs(priviledgeFilter);
        final Dataframe unprivileged = data.filterRowsByInputs(priviledgeFilter.negate());

        return groupDisparateImpactRatio(privileged, unprivileged, favorableOutput);
    }

    /**
     * Calculate disparate impact ratio (DIR).
     *
     * @param priviledged {@link Dataframe} with the priviledged groups
     * @param unpriviledged {@link Dataframe} with the unpriviledged groups
     * @param favorableOutput an output that is considered favorable / desirable
     * @return DIR, between 0 and 1
     */
    public static double groupDisparateImpactRatio(Dataframe priviledged, Dataframe unpriviledged, List<Output> favorableOutput) {
        final List<Value> favorableValues = favorableOutput.stream().map(Output::getValue).collect(Collectors.toUnmodifiableList());
        final Predicate<List<Value>> match = values -> values.equals(favorableValues);

        final double probabilityPrivileged = (double) priviledged.filterRowsByOutputs(match).getRowDimension() / (double) priviledged.getRowDimension();
        final double probabilityUnprivileged = (double) unpriviledged.filterRowsByOutputs(match).getRowDimension() / (double) unpriviledged.getRowDimension();

        return probabilityUnprivileged / probabilityPrivileged;
    }

    private static double getFavorableLabelProbability(Predicate<PredictionInput> groupSelector, List<PredictionInput> samples,
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

    /**
     * Calculate average odds difference.
     *
     * @param inputSelector selector for privileged group
     * @param outputSelector selector for favorable label
     * @param dataset dataset used to evaluate AOD
     * @param model model to be evaluated fairness-wise
     * @return average odds difference value
     * @throws ExecutionException if any error occurs during model prediction
     * @throws InterruptedException if timeout or other interruption issues occur during model prediction
     */
    public static double groupAverageOddsDifference(Predicate<PredictionInput> inputSelector,
            Predicate<PredictionOutput> outputSelector, Dataset dataset,
            PredictionProvider model)
            throws ExecutionException, InterruptedException {

        Dataset privileged = dataset.filterByInput(inputSelector);
        Map<String, Integer> privilegedCounts = countMatchingOutputSelector(privileged, model.predictAsync(privileged.getInputs()).get(), outputSelector);

        Dataset unprivileged = dataset.filterByInput(inputSelector.negate());
        Map<String, Integer> unprivilegedCounts = countMatchingOutputSelector(unprivileged, model.predictAsync(unprivileged.getInputs()).get(), outputSelector);

        double utp = unprivilegedCounts.get("tp");
        double utn = unprivilegedCounts.get("tn");
        double ufp = unprivilegedCounts.get("fp");
        double ufn = unprivilegedCounts.get("fn");

        double ptp = privilegedCounts.get("tp");
        double ptn = privilegedCounts.get("tn");
        double pfp = privilegedCounts.get("fp");
        double pfn = privilegedCounts.get("fn");

        return (utp / (utp + ufn) - ptp / (ptp + pfn + 1e-10)) / 2d + (ufp / (ufp + utn) - pfp / (pfp + ptn + 1e-10)) / 2;
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

    private static ConfusionMatrix calculateConfusionMatrix(Dataframe test, Dataframe truth, Predicate<List<Value>> positive) {

        final List<List<Value>> testOutputs = test.getOutputRows();
        final List<List<Value>> truthOutputs = truth.getOutputRows();

        return ConfusionMatrix.create(
                countTruePositives(testOutputs, truthOutputs, positive),
                countTrueNegatives(testOutputs, truthOutputs, positive),
                countFalsePositives(testOutputs, truthOutputs, positive),
                countFalseNegatives(testOutputs, truthOutputs, positive));
    }

    public static double groupAverageOddsDifference(final Dataframe samples,
            PredictionProvider model,
            final List<Integer> priviledgeColumns,
            final List<Value> priviledgeValues,
            final List<Value> positiveClass) throws ExecutionException, InterruptedException {

        final List<PredictionInput> inputs = samples.asPredictionInputs();
        final List<PredictionOutput> outputs = model.predictAsync(inputs).get();

        final Dataframe truth = Dataframe.createFrom(inputs, outputs);

        return groupAverageOddsDifference(samples, truth, priviledgeColumns, priviledgeValues, positiveClass);
    }

    public static double groupAverageOddsDifference(final Dataframe test,
            final Dataframe truth,
            final List<Integer> priviledgeColumns,
            final List<Value> priviledgeValues,
            final List<Value> positiveClass) {

        final Predicate<List<Value>> priviledgeFilter = values -> priviledgeColumns
                .stream().map(values::get)
                .collect(Collectors.toList())
                .equals(priviledgeValues);

        final Predicate<List<Value>> positive = values -> values.equals(positiveClass);

        final Dataframe testPriviledgeData = test.filterRowsByInputs(priviledgeFilter);
        final Dataframe testUnpriviledgeData = test.filterRowsByInputs(priviledgeFilter.negate());

        final Dataframe truthPriviledgeData = truth.filterRowsByInputs(priviledgeFilter);
        final Dataframe truthUnpriviledgeData = truth.filterRowsByInputs(priviledgeFilter.negate());

        final ConfusionMatrix ucm = calculateConfusionMatrix(testUnpriviledgeData, truthUnpriviledgeData, positive);
        final ConfusionMatrix pcm = calculateConfusionMatrix(testPriviledgeData, truthPriviledgeData, positive);

        double utp = ucm.getTruePositives();
        double utn = ucm.getTrueNegatives();
        double ufp = ucm.getFalsePositives();
        double ufn = ucm.getFalseNegatives();

        double ptp = pcm.getTruePositives();
        double ptn = pcm.getTrueNegatives();
        double pfp = pcm.getFalsePositives();
        double pfn = ucm.getFalseNegatives();

        return (utp / (utp + ufn) - ptp / (ptp + pfn + 1e-10)) / 2d + (ufp / (ufp + utn) - pfp / (pfp + ptn + 1e-10)) / 2;
    }

    /**
     * Count true / false favorable and true / false unfavorable outputs with respect to a specified output selector.
     * 
     * @param dataset dataset used to match predictions with labels
     * @param predictionOutputs predictions to match with the dataset labels
     * @param outputSelector selector to define positive labelled samples / predictions
     * @return a map containing counts for true positives ("tp"), true negatives ("tn"), false positives ("fp"), false negatives ("fn")
     */
    private static Map<String, Integer> countMatchingOutputSelector(Dataset dataset, List<PredictionOutput> predictionOutputs,
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

    /**
     * Calculate average predictive value difference.
     *
     * @param inputSelector selector for privileged group
     * @param outputSelector selector for favorable label
     * @param dataset dataset used to evaluate AOD
     * @param model model to be evaluated fairness-wise
     * @return average predictive value difference
     * @throws ExecutionException if any error occurs during model prediction
     * @throws InterruptedException if timeout or other interruption issues occur during model prediction
     */
    public static double groupAveragePredictiveValueDifference(Predicate<PredictionInput> inputSelector,
            Predicate<PredictionOutput> outputSelector, Dataset dataset,
            PredictionProvider model)
            throws ExecutionException, InterruptedException {

        Dataset privileged = dataset.filterByInput(inputSelector);
        Map<String, Integer> privilegedCounts = countMatchingOutputSelector(privileged, model.predictAsync(privileged.getInputs()).get(), outputSelector);

        double ptp = privilegedCounts.get("tp");
        double ptn = privilegedCounts.get("tn");
        double pfp = privilegedCounts.get("fp");
        double pfn = privilegedCounts.get("fn");

        Dataset unprivileged = dataset.filterByInput(inputSelector.negate());
        Map<String, Integer> unprivilegedCounts = countMatchingOutputSelector(unprivileged, model.predictAsync(unprivileged.getInputs()).get(), outputSelector);

        double utp = unprivilegedCounts.get("tp");
        double utn = unprivilegedCounts.get("tn");
        double ufp = unprivilegedCounts.get("fp");
        double ufn = unprivilegedCounts.get("fn");

        return (utp / (utp + ufp) - ptp / (ptp + pfp + 1e-10)) / 2d + (ufn / (ufn + utn) - pfn / (pfn + ptn + 1e-10)) / 2;
    }

    public static double groupAveragePredictiveValueDifference(final Dataframe samples,
            PredictionProvider model,
            final List<Integer> priviledgeColumns,
            final List<Value> priviledgeValues,
            final List<Value> positiveClass) throws ExecutionException, InterruptedException {

        final List<PredictionInput> inputs = samples.asPredictionInputs();
        final List<PredictionOutput> outputs = model.predictAsync(inputs).get();

        final Dataframe truth = Dataframe.createFrom(inputs, outputs);

        return groupAveragePredictiveValueDifference(samples, truth, priviledgeColumns, priviledgeValues, positiveClass);
    }

    /**
     * Calculate average predictive value difference.
     *
     */
    public static double groupAveragePredictiveValueDifference(final Dataframe test,
            final Dataframe truth,
            final List<Integer> priviledgeColumns,
            final List<Value> priviledgeValues,
            final List<Value> positiveClass) {

        final Predicate<List<Value>> priviledgeFilter = values -> priviledgeColumns
                .stream().map(values::get)
                .collect(Collectors.toList())
                .equals(priviledgeValues);

        final Predicate<List<Value>> positive = values -> values.equals(positiveClass);

        final Dataframe testPriviledgeData = test.filterRowsByInputs(priviledgeFilter);
        final Dataframe testUnpriviledgeData = test.filterRowsByInputs(priviledgeFilter.negate());

        final Dataframe truthPriviledgeData = truth.filterRowsByInputs(priviledgeFilter);
        final Dataframe truthUnpriviledgeData = truth.filterRowsByInputs(priviledgeFilter.negate());

        final ConfusionMatrix ucm = calculateConfusionMatrix(testUnpriviledgeData, truthUnpriviledgeData, positive);
        final ConfusionMatrix pcm = calculateConfusionMatrix(testPriviledgeData, truthPriviledgeData, positive);

        double utp = ucm.getTruePositives();
        double utn = ucm.getTrueNegatives();
        double ufp = ucm.getFalsePositives();
        double ufn = ucm.getFalseNegatives();

        double ptp = pcm.getTruePositives();
        double ptn = pcm.getTrueNegatives();
        double pfp = pcm.getFalsePositives();
        double pfn = ucm.getFalseNegatives();

        return (utp / (utp + ufp) - ptp / (ptp + pfp + 1e-10)) / 2d + (ufn / (ufn + utn) - pfn / (pfn + ptn + 1e-10)) / 2;
    }
}
