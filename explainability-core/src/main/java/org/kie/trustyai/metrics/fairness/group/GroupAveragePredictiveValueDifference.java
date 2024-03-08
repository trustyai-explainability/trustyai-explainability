package org.kie.trustyai.metrics.fairness.group;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.metrics.accuracy.ConfusionMatrix;
import org.kie.trustyai.metrics.fairness.FairnessMetricsUtils;

public class GroupAveragePredictiveValueDifference {
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
    public static double calculate(Predicate<PredictionInput> inputSelector,
            Predicate<PredictionOutput> outputSelector, Dataset dataset,
            PredictionProvider model)
            throws ExecutionException, InterruptedException {

        Dataset privileged = dataset.filterByInput(inputSelector);
        Map<String, Integer> privilegedCounts = FairnessMetricsUtils.countMatchingOutputSelector(privileged, model.predictAsync(privileged.getInputs()).get(), outputSelector);

        double ptp = privilegedCounts.get("tp");
        double ptn = privilegedCounts.get("tn");
        double pfp = privilegedCounts.get("fp");
        double pfn = privilegedCounts.get("fn");

        Dataset unprivileged = dataset.filterByInput(inputSelector.negate());
        Map<String, Integer> unprivilegedCounts = FairnessMetricsUtils.countMatchingOutputSelector(unprivileged, model.predictAsync(unprivileged.getInputs()).get(), outputSelector);

        double utp = unprivilegedCounts.get("tp");
        double utn = unprivilegedCounts.get("tn");
        double ufp = unprivilegedCounts.get("fp");
        double ufn = unprivilegedCounts.get("fn");

        return (utp / (utp + ufp) - ptp / (ptp + pfp + 1e-10)) / 2d + (ufn / (ufn + utn) - pfn / (pfn + ptn + 1e-10)) / 2;
    }

    public static double calculate(final Dataframe samples,
            PredictionProvider model,
            final List<Integer> priviledgeColumns,
            final List<Value> priviledgeValues,
            final List<Value> positiveClass) throws ExecutionException, InterruptedException {

        final List<PredictionInput> inputs = samples.asPredictionInputs();
        final List<PredictionOutput> outputs = model.predictAsync(inputs).get();

        final Dataframe truth = Dataframe.createFrom(inputs, outputs);

        return calculate(samples, truth, priviledgeColumns, priviledgeValues, positiveClass);
    }

    /**
     * Calculate average predictive value difference.
     *
     */
    public static double calculate(final Dataframe test,
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

        final ConfusionMatrix ucm = FairnessMetricsUtils.calculateConfusionMatrix(testUnpriviledgeData, truthUnpriviledgeData, positive);
        final ConfusionMatrix pcm = FairnessMetricsUtils.calculateConfusionMatrix(testPriviledgeData, truthPriviledgeData, positive);

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
