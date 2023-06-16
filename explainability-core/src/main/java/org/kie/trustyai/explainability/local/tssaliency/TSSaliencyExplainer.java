package org.kie.trustyai.explainability.local.tssaliency;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.kie.trustyai.explainability.local.LocalExplainer;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.SaliencyResults;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

public class TSSaliencyExplainer implements LocalExplainer<SaliencyResults> {

    private double[] baseValue; // check
    private int gradientSamples; // Number of samples for gradient estimation
    private int steps; // Number of steps in convex path
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

    // f = Multivariate Time Series Model Inference Function
    // T; F = Model’s time and feature input dimension
    // x = fxt;jgt2[T];j2[F] : Time series instance
    // b = fbjgj2[F]: Feature’s base values (optional)
    // ng = Number of samples for gradient estimation
    // n = Number of steps in convex path

    public TSSaliencyExplainer(double[] baseValue, int gradientSamples, int steps, int randomSeed) {

        this.baseValue = baseValue;
        this.gradientSamples = gradientSamples;
        this.steps = steps;
        this.randomSeed = randomSeed;
    }

    @Override
    public CompletableFuture<SaliencyResults> explainAsync(Prediction prediction, PredictionProvider model,
            Consumer<SaliencyResults> intermediateResultsConsumer) {

        // Input:
        // f = Multivariate Time Series Model Inference Function CHECK
        // T; F = Model’s time and feature input dimension
        // x(t, j) x = fxt;jgt2[T];j2[F] : Time series instance
        // b = fbjgj2[F]: Feature’s base values (optional)
        // ng = Number of samples for gradient estimation
        // nalpha = Number of steps in convex path

        PredictionInput predictionInputs = prediction.getInput();

        PredictionOutput predictionOutput = prediction.getOutput();

        List<Feature> features = predictionInputs.getFeatures();

        if (baseValue.length == 0) {
            baseValue = calcBaseValue(features);
        }

        System.out.println("baseValues = ");
        for (int i = 0; i < baseValue.length; i++) {
            System.out.print(baseValue[i] + " ");
        }
        System.out.println();

        Feature[] featuresArray = features.toArray(new Feature[0]);


        // alpha = [ n(alpha) ] / n(alpha)
        double[] nalpha = new double[steps];
        for (int s = 0; s < steps; s++) {
            nalpha[s] = s / ((double) steps - 1);
        }

        System.out.println("nalpha = ");
        for (int i = 0; i < nalpha.length; i++) {
            System.out.print(nalpha[i] + " ");
        }
        System.out.println();

        // SCORE = 0
        // for i 1 to n do
        //   Compute affine sample:
        //   s = alpha(i) * X + (1 - alpha(i)) * (1(T) * transpose(b))



        //   T b>)
        //   Compute Monte Carlo gradient (per time and feature dimension):
        //   g = MC_GRADIENT(s; f; ng)
        //   Update Score:
        //   SCORE = SCORE + g
        // n end for

        for (int i = 0; i < steps; i++) {

        }

        return null;

    }

    private double[] calcBaseValue(List<Feature> features) {

        // 1/T sum(1..T) x(t, j)

        Feature[] featuresArray = features.toArray(new Feature[0]);

        Feature feature0 = featuresArray[0];
        assert feature0.getType() == Type.VECTOR;

        Value feature0Value = feature0.getValue();

        double[] feature0Values = feature0Value.asVector();
        int featureLength = feature0Values.length;

        double[] retval = new double[featureLength];

        for (int i = 0; i < featureLength; i++) {
            retval[i] = 0.0;
        }

        for (int j = 0; j < featuresArray.length; j++) {

            Feature feature = featuresArray[j];
            assert feature.getType() == Type.VECTOR;

            Value featureValue = feature.getValue();
            double[] featureArray = featureValue.asVector();
            assert featureArray.length == featureLength;

            for (int i = 0; i < featureLength; i++) {
                retval[i] += featureArray[i];
            }
        }

        for (int i = 0; i < featureLength; i++) {
            retval[i] /= featuresArray.length;
        }

        return retval;
    }
}
