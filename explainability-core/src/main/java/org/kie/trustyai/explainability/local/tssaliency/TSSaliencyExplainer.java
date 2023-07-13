package org.kie.trustyai.explainability.local.tssaliency;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.kie.trustyai.explainability.local.LocalExplainer;
import org.kie.trustyai.explainability.local.TimeSeriesExplainer;
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

public class TSSaliencyExplainer implements TimeSeriesExplainer<SaliencyResults> {

    private double[] baseValue; // check
    private int ng; // Number of samples for gradient estimation
    public int nalpha; // Number of steps in convex path
    private int randomSeed;

    public TSSaliencyExplainer(double[] baseValue, int ng, int nalpha, int randomSeed) {
        this.baseValue = baseValue;
        this.ng = ng;
        this.nalpha = nalpha;
        this.randomSeed = randomSeed;

    }

    @Override
    public CompletableFuture<SaliencyResults> explainAsync(Prediction prediction, PredictionProvider model, Consumer<SaliencyResults> intermediateResultsConsumer) {
        throw new UnsupportedOperationException();
     }

    @Override
    public CompletableFuture<SaliencyResults> explainAsync(List<Prediction> predictions, PredictionProvider model,
            Consumer<SaliencyResults> intermediateResultsConsumer) {

        try {

            final Prediction prediction = predictions.get(0);

            PredictionInput predictionInputs = prediction.getInput();

            PredictionOutput predictionOutput = prediction.getOutput();

            // RealVector pi =
            // MatrixUtilsExtensions.vectorFromPredictionInput(predictionInputs);

            List<Output> outputs = predictionOutput.getOutputs();
            Output output = outputs.get(0);

            // List<Feature> features = predictionInputs.getFeatures();

            double[][] x = matrixFromFeatures(predictionInputs);

            RealMatrix pi = MatrixUtils.createRealMatrix(x);

            // Feature[] featuresArray = features.toArray(new Feature[0]);
            // int T = featuresArray.length;

            // Feature feature0 = featuresArray[0];
            // assert feature0.getType() == Type.VECTOR;

            // Value feature0Value = feature0.getValue();

            // double[] feature0Values = feature0Value.asVector();
            // int F = feature0Values.length;

            int T = x.length;
            int F = x[0].length;

            // alpha = [ n(alpha) ] / n(alpha)
            double[] alpha = new double[nalpha];
            for (int s = 0; s < nalpha; s++) {
                alpha[s] = s / ((double) nalpha - 1);
            }

            // double[][] x = new double[T][F];
            // for (int t = 0; t < T; t++) {
            // Feature feature = featuresArray[t];
            // Value value = feature.getValue();
            // double[] elements = value.asVector();

            // for (int f = 0; f < F; f++) {
            // x[t][f] = elements[f];
            // }
            // }

            if (baseValue.length == 0) {
                baseValue = calcBaseValue(x);
            }

            // SCORE = 0
            double[][] score = new double[T][F];
            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    score[t][f] = 0.0;
                }
            }

            int numberCores = Runtime.getRuntime().availableProcessors();

            TSSaliencyThreadInfo[] threadInfo = new TSSaliencyThreadInfo[numberCores];
            for (int t = 0; t < numberCores; t++) {
                threadInfo[t] = new TSSaliencyThreadInfo();
                threadInfo[t].alphaList = new LinkedList<Integer>();
                // alphaList[t] = new LinkedList<Integer>();
            }

            // int currentThreadNumber = 0;
            for (int i = 0; i < nalpha; i++) {
                threadInfo[i % numberCores].alphaList.add(Integer.valueOf(i));
            }

            for (int t = 0; t < numberCores; t++) {
                threadInfo[t].runner = new TSSaliencyRunner(x, alpha, baseValue, score, model, this,
                        threadInfo[t].alphaList);

                threadInfo[t].thread = new Thread(threadInfo[t].runner, "Runner" + t);
                threadInfo[t].thread.start();
            }

            for (int i = 0; i < numberCores; i++) {
                threadInfo[i].thread.join();
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

            final Saliency saliency = new Saliency(output, featureImportances);
            saliencies.put(output.getName(), saliency);

            CompletableFuture<SaliencyResults> retval = new CompletableFuture<SaliencyResults>();

            retval.complete(saliencyResults);

            // System.out.println("normal avg time = " + ((double) totalNormalTime) /
            // totalNormalCount);
            // System.out.println("normal total time = " + ((double) totalNormalTime) /
            // 1e9);
            // System.out.println("normal total calls = " + totalNormalCount);

            return retval;
        } catch (

        Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // public static RealMatrix realMatrixFromFeatures(List<Feature> ps) {
    // // return MatrixUtils.createRealMatrix(
    // double[][] vect = ps.stream()
    // // .map(p -> p.toArray(new Feature[0]))

    // .map(f -> f.getValue().asVector()).toArray(double[][]::new);

    // // .map(p -> p.getFeatures().stream()
    // // .mapToDouble(f -> f.getValue().asNumber())
    // // .toArray())
    // // .toArray(double[][]::new));

    // return MatrixUtils.createRealMatrix(vect);
    // }

    public static double[][] matrixFromFeatures(PredictionInput pi) {

        List<Feature> ps = pi.getFeatures();

        // return MatrixUtils.createRealMatrix(
        double[][] vect = ps.stream()
                // .map(p -> p.toArray(new Feature[0]))

                .map(f -> f.getValue().asVector()).toArray(double[][]::new);

        // .map(p -> p.getFeatures().stream()
        // .mapToDouble(f -> f.getValue().asNumber())
        // .toArray())
        // .toArray(double[][]::new));

        return vect;
    }

    private double[] calcBaseValue(double[][] x) {

        // 1/T sum(1..T) x(t, j)

        int T = x.length;
        int F = x[0].length;

        double[] retval = new double[F];

        // for (int i = 0; i < F; i++) {
        // retval[i] = 0.0;
        // }

        Arrays.fill(retval, 0.0);

        for (int t = 0; t < T; t++) {

            // Feature feature = featuresArray[j];
            // assert feature.getType() == Type.VECTOR;

            // Value featureValue = feature.getValue();
            // double[] featureArray = featureValue.asVector();
            // assert featureArray.length == F;

            for (int f = 0; f < F; f++) {
                retval[f] += x[t][f];
            }
        }

        for (int i = 0; i < F; i++) {
            retval[i] /= T;
        }

        return retval;
    }

    public double[][] monteCarloGradient(double[][] x, PredictionProvider model) throws Exception {

        final double SIGMA = 10.0;
        final double MU = 0.01;

        int T = x.length;
        int F = x[0].length;

        // long totalNormalCount = 0;
        // long totalNormalTime = 0;

        // double[] x = baseValue;
        // gradientSamples mu

        // NormalDistribution N = new NormalDistribution(0.0, SIGMA);
        Random r = new Random();
        // N.reseedRandomGenerator(randomSeed);

        // Sample ng independent data points from the unit sphere

        double U[][][] = new double[ng][T][F];

        for (int n = 0; n < ng; n++) {

            // long startNano = System.nanoTime();

            double sum = 0.0;
            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    // long normalStart = System.nanoTime();
                    // U[n][t][f] = N.sample();
                    U[n][t][f] = r.nextGaussian() * SIGMA;
                    // long normalEnd = System.nanoTime();
                    // totalNormalCount++;
                    // totalNormalTime += (normalEnd - normalStart);

                    sum += (U[n][t][f]) * (U[n][t][f]);
                }
            }

            double L2norm = Math.sqrt(sum);

            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    U[n][t][f] = (U[n][t][f]) / L2norm;
                }
            }

            // long endNano = System.nanoTime();

            // double time = (endNano - startNano) / 1e9;
            // System.out.println(n + " monte carlo inner time = " + time);
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
