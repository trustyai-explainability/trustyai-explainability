package org.kie.trustyai.explainability.local.tssaliency;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.kie.trustyai.explainability.local.LocalExplainer;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureImportance;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.Saliency;
import org.kie.trustyai.explainability.model.SaliencyResults;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.SaliencyResults.SourceExplainer;

import org.apache.commons.math3.distribution.NormalDistribution;

public class TSSaliencyExplainer implements LocalExplainer<SaliencyResults> {

    private double[] baseValue; // check
    private int gradientSamples; // Number of samples for gradient estimation n(g)
    private int steps; // Number of steps in convex path
    // private Object gradientFunction;
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

        try {
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

            Feature[] featuresArray = features.toArray(new Feature[0]);
            int T = featuresArray.length;

            Feature feature0 = featuresArray[0];
            assert feature0.getType() == Type.VECTOR;

            Value feature0Value = feature0.getValue();

            double[] feature0Values = feature0Value.asVector();
            int F = feature0Values.length;

            if (baseValue.length == 0) {
                baseValue = calcBaseValue(featuresArray, T, F);
            }

            System.out.println("baseValues = ");
            for (int i = 0; i < baseValue.length; i++) {
                System.out.print(baseValue[i] + " ");
            }
            System.out.println();

            // alpha = [ n(alpha) ] / n(alpha)
            double[] alpha = new double[steps];
            for (int s = 0; s < steps; s++) {
                alpha[s] = s / ((double) steps - 1);
            }

            System.out.println("alpha = ");
            for (int i = 0; i < alpha.length; i++) {
                System.out.print(alpha[i] + " ");
            }
            System.out.println();

            double[][] x = new double[T][F];
            for (int t = 0; t < T; t++) {
                Feature feature = featuresArray[t];
                Value value = feature.getValue();
                double[] elements = value.asVector();

                for (int f = 0; f < F; f++) {
                    x[t][f] = elements[f];
                }
            }

            // SCORE = 0
            double[][] score = new double[T][F];
            for (int t = 0; t < T; t++) {

                for (int f = 0; f < F; f++) {
                    score[t][f] = 0.0;
                }
            }

            // for i 1 to n do
            for (int i = 0; i < steps; i++) {

                // Compute affine sample:
                // s = alpha(i) * X + (1 - alpha(i)) * (1(T) * transpose(b))

                // Giridhar Ganapavarapu
                // 4:57 PM
                // Extend this base value to shape T dimensions.. replicate base value by T

                // double[][] replicateB = new double[T][F];
                // for (int t = 0; t < T; t++) {
                // for (int f = 0; f < F; f++) {
                // replicateB[t][f] = baseValue[f];
                // }
                // }

                double[][] s = new double[T][F];
                for (int t = 0; t < T; t++) {

                    for (int f = 0; f < F; f++) {
                        s[t][f] = alpha[i] * x[t][f] + (1.0 - alpha[i]) * baseValue[f];
                    }
                }

                // Compute Monte Carlo gradient (per time and feature dimension):

                // g = MC_GRADIENT(s; f; ng)
                double[][] g = monteCarloGradient(s, model, gradientSamples);

                // Update Score:
                for (int t = 0; t < T; t++) {

                    for (int f = 0; f < F; f++) {
                        score[t][f] = score[t][f] + g[t][f] / steps;
                    }
                }

                // n end for
            }

            // IG(t,j) = x(t,j) * SCORE(t,j)

            Map<String, Saliency> saliencies = new HashMap<String, Saliency>();

            SaliencyResults saliencyResults = new SaliencyResults(saliencies, SourceExplainer.TSSALIENCY);

            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    String name = "IG[" + t + "][" + f + "]";
                    double val = x[t][f] * score[t][f];
                    Output output = new Output(name, Type.NUMBER, null, val);
                    Saliency saliency = new Saliency(output, new LinkedList<FeatureImportance>());
                    saliencies.put(name, saliency);
                }
            }

            CompletableFuture<SaliencyResults> retval = new CompletableFuture<SaliencyResults>();

            retval.complete(saliencyResults);

            return retval;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private double[] calcBaseValue(Feature[] featuresArray, int T, int F) {

        // 1/T sum(1..T) x(t, j)

        double[] retval = new double[F];

        for (int i = 0; i < F; i++) {
            retval[i] = 0.0;
        }

        for (int j = 0; j < T; j++) {

            Feature feature = featuresArray[j];
            assert feature.getType() == Type.VECTOR;

            Value featureValue = feature.getValue();
            double[] featureArray = featureValue.asVector();
            assert featureArray.length == F;

            for (int i = 0; i < F; i++) {
                retval[i] += featureArray[i];
            }
        }

        for (int i = 0; i < F; i++) {
            retval[i] /= T;
        }

        return retval;
    }

    private double[][] monteCarloGradient(double[][] x, PredictionProvider model, int ng) throws Exception {

        final double SIGMA = 10.0;
        final double MU = 0.01;

        int T = x.length;
        int F = x[0].length;

        // double[] x = baseValue;
        // gradientSamples mu

        NormalDistribution N = new NormalDistribution(0.0, SIGMA);

        // Sample ng independent data points from the unit sphere

        double U[][][] = new double[ng][T][F];

        for (int n = 0; n < ng; n++) {

            double sum = 0.0;
            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    U[n][t][f] = N.sample();
                    sum += (U[n][t][f]) * (U[n][t][f]);
                }
            }

            double L2norm = Math.sqrt(sum);

            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    U[n][t][f] = (U[n][t][f]) / L2norm;
                }
            }
        }

        double[] diff = new double[ng];

        List<PredictionInput> inputs = new LinkedList<PredictionInput>();

        for (int n = 0; n < ng; n++) {

            // dfs = f(x + u * sample) - f(x)

            List<Feature> features2delta = new LinkedList<Feature>();

            for (int t = 0; t < T; t++) {

                double[] feature3Array = new double[F];

                for (int f = 0; f < F; f++) {
                    feature3Array[f] = x[t][f] + MU * U[n][t][f];
                }

                Feature feature3 = new Feature("xdelta" + t, Type.VECTOR, new Value(feature3Array));
                features2delta.add(feature3);
            }

            PredictionInput inputDelta = new PredictionInput(features2delta);
            inputs.add(inputDelta);

            List<Feature> features2 = new LinkedList<Feature>();

            for (int t = 0; t < T; t++) {

                double[] feature3Array = new double[F];

                for (int f = 0; f < F; f++) {
                    feature3Array[f] = x[t][f];
                }

                Feature feature3 = new Feature("x" + t, Type.VECTOR, new Value(feature3Array));
                features2.add(feature3);
            }

            PredictionInput input = new PredictionInput(features2);
            inputs.add(input);
        }

        CompletableFuture<List<PredictionOutput>> result = model.predictAsync(inputs);
        List<PredictionOutput> results = result.get();

        for (int i = 0; i < results.size(); i += 2) {
            PredictionOutput fxDeltaPredictionOutput = results.get(i);
            PredictionOutput fxPredictionOutput = results.get(i + 1);

            List<Output> fxDeltas = fxDeltaPredictionOutput.getOutputs();
            List<Output> fxs = fxPredictionOutput.getOutputs();

            Output[] fxDelta = fxDeltas.toArray(new Output[0]);
            Output[] fx = fxs.toArray(new Output[0]);

            double fxDeltaScore = fxDelta[0].getScore();
            double fxScore = fx[0].getScore();

            diff[i / 2] = fxDeltaScore - fxScore;
        }

        // g(t,j) = (T * F) / ng * sum(ng) (diff * U) / MU)

        double[][] retval = new double[T][F];

        double mult = T * F / ng;

        for (int t = 0; t < T; t++) {
            for (int f = 0; f < F; f++) {

                double gsum = 0.0;

                for (int n = 0; n < ng; n++) {
                    // System.out.println(n + "," + t + "," + f);
                    double term = diff[n] * U[n][t][f];
                    double term2 = term / MU;
                    gsum += term2;
                }

                retval[t][f] = mult * gsum;
            }
        }

        return retval;
    }
}
