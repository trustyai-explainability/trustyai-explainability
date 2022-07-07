package org.kie.trustyai.explainability.local.counterfactual;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.kie.trustyai.explainability.model.CounterfactualPrediction;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

public class CounterfactualUtils {

    public static final Long MAX_RUNNING_TIME_SECONDS = 60L;
    public static final long predictionTimeOut = 20L;
    public static final TimeUnit predictionTimeUnit = TimeUnit.MINUTES;
    public static final Long DEFAULT_STEPS = 30_000L;
    public static final double DEFAULT_GOAL_THRESHOLD = 0.01;


    public static CounterfactualResult runCounterfactualSearch(Long randomSeed, List<Output> goal,
            List<Feature> features,
            PredictionProvider model,
            double goalThresold) throws InterruptedException, ExecutionException, TimeoutException {
        return runCounterfactualSearch(randomSeed, goal, features, model, goalThresold, DEFAULT_STEPS);
    }

    public static CounterfactualResult runCounterfactualSearch(Long randomSeed, List<Output> goal,
            List<Feature> features,
            PredictionProvider model,
            double goalThresold,
            long steps) throws InterruptedException, ExecutionException, TimeoutException {
        final TerminationConfig terminationConfig = new TerminationConfig().withScoreCalculationCountLimit(steps);
        final SolverConfig solverConfig = SolverConfigBuilder
                .builder().withTerminationConfig(terminationConfig).build();
        solverConfig.setRandomSeed(randomSeed);
        solverConfig.setEnvironmentMode(EnvironmentMode.REPRODUCIBLE);
        final CounterfactualConfig counterfactualConfig = new CounterfactualConfig();
        counterfactualConfig.withSolverConfig(solverConfig).withGoalThreshold(goalThresold);
        final CounterfactualExplainer explainer = new CounterfactualExplainer(counterfactualConfig);
        final PredictionInput input = new PredictionInput(features);
        PredictionOutput output = new PredictionOutput(goal);
        Prediction prediction =
                new CounterfactualPrediction(input,
                        output,
                        null,
                        UUID.randomUUID(),
                        null);
        return explainer.explainAsync(prediction, model)
                .get(CounterfactualUtils.predictionTimeOut, CounterfactualUtils.predictionTimeUnit);
    }
}
