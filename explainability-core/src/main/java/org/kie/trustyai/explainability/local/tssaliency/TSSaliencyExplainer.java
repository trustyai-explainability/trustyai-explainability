package org.kie.trustyai.explainability.local.tssaliency;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.kie.trustyai.explainability.local.LocalExplainer;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionProvider;

public class TSSaliencyExplainer implements LocalExplainer<IntegratedGradient> {

    private PredictionProvider model;
    private int inputLength;
    private List<Feature> x;
    private float[] baseValue; // check
    private int numSamples;
    private int gradientSamples;
    private Object gradientFunction;
    private int randomSeed;

    // def __init__(
    // self,
    // model: Callable,
    // input_length: int,
    // feature_names: List[str],
    // base_value: List[float] = None,
    // n_samples: int = 50,
    // gradient_samples: int = 25,
    // gradient_function: Callable = None,
    // random_seed: int = 22,
    // ):

    public TSSaliencyExplainer(PredictionProvider model, int inputLength, List<Feature> x, float[] baseValue,
            int numSamples, int gradientSamples, Object gradientFunction, int randomSeed) {

        this.model = model;
        this.inputLength = inputLength;
        this.x = x;
        this.baseValue = baseValue;
        this.numSamples = numSamples;
        this.gradientSamples = gradientSamples;
        this.gradientFunction = gradientFunction;
        this.randomSeed = randomSeed;
    }

    // CompletableFuture<IntegratedGradient> explainAsync(Prediction prediction,
    // PredictionProvider model) {
    // return explainAsync(prediction, model, unused -> {
    // /* NOP */
    // });
    // };

    // public interface Prediction {

    // PredictionInput getInput();

    // PredictionOutput getOutput();

    // UUID getExecutionId();
    // }

    // @FunctionalInterface
    // public interface PredictionProvider {

    // /**
    // * Perform a batch of predictions, given a batch of inputs.
    // *
    // * @param inputs the input batch
    // * @return a batch of prediction outputs
    // */
    // CompletableFuture<List<PredictionOutput>> predictAsync(List<PredictionInput>
    // inputs);
    // }

    @Override
    public CompletableFuture<IntegratedGradient> explainAsync(Prediction prediction, PredictionProvider model,
            Consumer<IntegratedGradient> intermediateResultsConsumer) {

        // Input:
        // f = Multivariate Time Series Model Inference Function CHECK
        // T; F = Model’s time and feature input dimension
        // x(t, j) x = fxt;jgt2[T];j2[F] : Time series instance
        // b = fbjgj2[F]: Feature’s base values (optional)
        // ng = Number of samples for gradient estimation
        // n = Number of steps in convex path

        // fbj = 1
        // T
        // PT
        // t=1 xt;jgj2[F]

        // if (baseValue.length == 0) {
        //     baseValue = calcBaseValue();
        // }

        return null;

    }

    // private double[] calcBaseValue() {
    //     // 1/T sum(1..T) x(t, j)

    //     for (i = 0; i < )



    // }
}
