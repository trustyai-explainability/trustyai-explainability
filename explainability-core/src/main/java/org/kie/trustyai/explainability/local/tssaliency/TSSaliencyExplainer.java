package org.kie.trustyai.explainability.local.tssaliency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.commons.math3.distribution.NormalDistribution;
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
import org.kie.trustyai.explainability.model.SaliencyResults.SourceExplainer;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

public class TSSaliencyExplainer implements LocalExplainer<SaliencyResults> {

    private double[] baseValue; // check
    private int ng; // Number of samples for gradient estimation
    private int nalpha; // Number of steps in convex path
    private int randomSeed;

    public TSSaliencyExplainer(double[] baseValue, int ng, int nalpha, int randomSeed) {
        this.baseValue = baseValue;
        this.ng = ng;
        this.nalpha = nalpha;
        this.randomSeed = randomSeed;
    }

    @Override
    public CompletableFuture<SaliencyResults> explainAsync(Prediction prediction, PredictionProvider model,
            Consumer<SaliencyResults> intermediateResultsConsumer) {

        try {
            PredictionInput predictionInputs = prediction.getInput();

            PredictionOutput predictionOutput = prediction.getOutput();
            List<Output> outputs = predictionOutput.getOutputs();
            Output output = outputs.get(0);

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

            // alpha = [ n(alpha) ] / n(alpha)
            double[] alpha = new double[nalpha];
            for (int s = 0; s < nalpha; s++) {
                alpha[s] = s / ((double) nalpha - 1);
            }

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
            for (int i = 0; i < nalpha; i++) {

                // Compute affine sample:
                // s = alpha(i) * X + (1 - alpha(i)) * (1(T) * transpose(b))

                double[][] s = new double[T][F];
                for (int t = 0; t < T; t++) {

                    for (int f = 0; f < F; f++) {
                        s[t][f] = alpha[i] * x[t][f] + (1.0 - alpha[i]) * baseValue[f];
                    }
                }

                // Compute Monte Carlo gradient (per time and feature dimension):

                // g = MC_GRADIENT(s; f; ng)
                double[][] g = monteCarloGradient(s, model, output);

                // Update Score:
                for (int t = 0; t < T; t++) {

                    for (int f = 0; f < F; f++) {
                        score[t][f] = score[t][f] + g[t][f] / nalpha;
                    }
                }

                // n end for
            }

            // IG(t,j) = x(t,j) * SCORE(t,j)

            Map<String, Saliency> saliencies = new HashMap<String, Saliency>();

            SaliencyResults saliencyResults = new SaliencyResults(saliencies, SourceExplainer.TSSALIENCY);

            double[][] scoreResult = new double[T][F];
            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    // String name = "IG[" + t + "][" + f + "]";
                    scoreResult[t][f] = (x[t][f] - baseValue[f]) * score[t][f];
                }
            }

            FeatureImportance featureImportance = new FeatureImportance(predictionInputs.getFeatures().get(0),
                    scoreResult, 0.0);
            List<FeatureImportance> featureImportances = new ArrayList<FeatureImportance>(1);
            featureImportances.add(featureImportance);

            Saliency saliency = new Saliency(output, featureImportances);
            saliencies.put("result", saliency);

            CompletableFuture<SaliencyResults> retval = new CompletableFuture<SaliencyResults>();

            retval.complete(saliencyResults);

            return retval;
        } catch (

        Exception e) {
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

    private double[][] monteCarloGradient(double[][] x, PredictionProvider model, Output output) throws Exception {

        final double SIGMA = 10.0;
        final double MU = 0.01;

        int T = x.length;
        int F = x[0].length;

        // double[] x = baseValue;
        // gradientSamples mu

        NormalDistribution N = new NormalDistribution(0.0, SIGMA);
        // N.reseedRandomGenerator(randomSeed);

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

        }

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

        CompletableFuture<List<PredictionOutput>> result = model.predictAsync(inputs);
        List<PredictionOutput> results = result.get();

        // model prediction for original x
        PredictionOutput fxPredictionOutput = results.get(results.size() - 1);
        List<Output> fxs = fxPredictionOutput.getOutputs();
        Output[] fx = fxs.toArray(new Output[0]);
        double fxScore = fx[0].getScore();

        for (int i = 0; i < results.size() - 1; i++) {
            PredictionOutput fxDeltaPredictionOutput = results.get(i);
            // PredictionOutput fxPredictionOutput = results.get(i + 1);

            List<Output> fxDeltas = fxDeltaPredictionOutput.getOutputs();
            // List<Output> fxs = fxPredictionOutput.getOutputs();

            Output[] fxDelta = fxDeltas.toArray(new Output[0]);
            // Output[] fx = fxs.toArray(new Output[0]);

            double fxDeltaScore = fxDelta[0].getScore();

            diff[i] = fxDeltaScore - fxScore;
        }

        // g(t,j) = (T * F) / ng * sum(ng) (diff * U) / MU)

        double[][] retval = new double[T][F];

        double mult = T * F / ((double) ng);

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
