package org.kie.trustyai.explainability.local.tssaliency;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.*;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.kie.trustyai.explainability.local.TimeSeriesExplainer;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.model.SaliencyResults.SourceExplainer;

public class TSSaliencyExplainer implements TimeSeriesExplainer<SaliencyResults> {

    final public int nalpha; // Number of steps in convex path
    final double sigma; // standard deviation
    final double mu; // Step size for gradient estimation
    final private int ng; // Number of samples for gradient estimation
    final private RandomGenerator randomGenerator; // random number generatr
    private RealVector baseValue; // Feature’s base values

    public TSSaliencyExplainer(double[] baseValue, int ng, int nalpha, int randomSeed, double sigma, double mu) {
        this.baseValue = new ArrayRealVector(baseValue);
        this.ng = ng;
        this.nalpha = nalpha;
        this.randomGenerator = new Well19937c(randomSeed);
        this.sigma = sigma;
        this.mu = mu;
    }

    @Override
    public CompletableFuture<SaliencyResults> explainAsync(Prediction prediction, PredictionProvider model,
            Consumer<SaliencyResults> intermediateResultsConsumer) {
        final List<Prediction> predictionList = new ArrayList<Prediction>(1);

        predictionList.add(prediction);

        return explainAsync(predictionList, model, intermediateResultsConsumer);
    }

    @Override
    public CompletableFuture<SaliencyResults> explainAsync(List<Prediction> predictions, PredictionProvider model,
            Consumer<SaliencyResults> intermediateResultsConsumer) {

        try {

            final Map<String, Saliency> saliencies = new HashMap<String, Saliency>();

            final SaliencyResults saliencyResults = new SaliencyResults(saliencies, SourceExplainer.TSSALIENCY);

            for (Prediction prediction : predictions) {

                final PredictionInput predictionInputs = prediction.getInput();

                final PredictionOutput predictionOutput = prediction.getOutput();

                final List<Output> outputs = predictionOutput.getOutputs();
                final Output output = outputs.get(0);

                final List<Feature> features = predictionInputs.getFeatures();

                final Feature[] featuresArray = features.toArray(new Feature[0]);
                final int T = featuresArray.length;

                // Get the first feature, to calculate the number of features
                final Feature feature0 = featuresArray[0];
                assert feature0.getType() == Type.VECTOR;

                final Value feature0Value = feature0.getValue();

                final double[] feature0Values = feature0Value.asVector();

                // F is the number of features
                final int F = feature0Values.length;

                if (baseValue.getDimension() == 0) {
                    baseValue = calcBaseValue(featuresArray, T, F);
                }

                // Create a sequence from 0 to nalpha-1
                final RealVector sequence = new ArrayRealVector(nalpha);
                for (int s = 0; s < nalpha; s++) {
                    sequence.setEntry(s, s);
                }

                // Divide the sequence by (nalpha - 1)
                final RealVector alpha = sequence.mapDivide(nalpha - 1.0);

                // A nested array of doubles, where each element is a timepoint
                final RealMatrix x = MatrixUtils.createRealMatrix(T, F);
                for (int t = 0; t < T; t++) {
                    final Feature feature = featuresArray[t];
                    final Value value = feature.getValue();
                    final double[] elements = value.asVector();
                    x.setRow(t, elements);
                }

                // Creating a matrix from baseValue with T identical rows
                final RealMatrix baseValueMatrix = new Array2DRowRealMatrix(T, baseValue.getDimension());
                for (int rowIndex = 0; rowIndex < T; rowIndex++) {
                    baseValueMatrix.setRowVector(rowIndex, baseValue);
                }

                final int numberCores = Runtime.getRuntime().availableProcessors();

                final TSSaliencyThreadInfo[] threadInfo = new TSSaliencyThreadInfo[numberCores];
                for (int t = 0; t < numberCores; t++) {
                    threadInfo[t] = new TSSaliencyThreadInfo();
                    threadInfo[t].alphaList = new LinkedList<Integer>();
                }

                for (int i = 0; i < nalpha; i++) {
                    threadInfo[i % numberCores].alphaList.add(Integer.valueOf(i));
                }

                // Elements are initialised with zero
                final RealMatrix score = MatrixUtils.createRealMatrix(T, F);

                for (int t = 0; t < numberCores; t++) {

                    threadInfo[t].runner = new TSSaliencyRunner(x, alpha, baseValueMatrix, score, model, this,
                            threadInfo[t].alphaList);

                    threadInfo[t].thread = new Thread(threadInfo[t].runner, "Runner" + t);
                    threadInfo[t].thread.start();
                }

                for (int i = 0; i < numberCores; i++) {
                    threadInfo[i].thread.join();
                }

                // IG(t,j) = x(t,j) * SCORE(t,j)

                final RealMatrix xMinusBaseValue = x.subtract(baseValueMatrix);

                final RealMatrix scoreResult = MatrixUtils.createRealMatrix(T, F);
                final RealMatrix fscore = score;
                IntStream.range(0, T).forEach(
                        t -> scoreResult.setRowVector(t,
                                xMinusBaseValue.getRowVector(t).ebeMultiply(fscore.getRowVector(t))));

                // assume 1 Feature in predictionInputs

                final FeatureImportance featureImportance = new FeatureImportance(predictionInputs.getFeatures().get(0),
                        scoreResult.getData(), 0.0);

                final List<FeatureImportance> featureImportances = new ArrayList<FeatureImportance>(1);
                featureImportances.add(featureImportance);

                final Saliency saliency = new Saliency(output, featureImportances);
                saliencies.put(output.getName(), saliency);
            }

            final CompletableFuture<SaliencyResults> retval = new CompletableFuture<SaliencyResults>();

            retval.complete(saliencyResults);

            return retval;
        } catch (

        Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }

    private RealVector calcBaseValue(Feature[] featuresArray, int T, int numberFeatures) {

        // 1/T sum(1..T) x(t, j)

        RealVector retval = new ArrayRealVector(numberFeatures);

        for (int t = 0; t < T; t++) {

            final Feature feature = featuresArray[t];
            assert feature.getType() == Type.VECTOR;

            final Value featureValue = feature.getValue();
            final double[] featureArray = featureValue.asVector();
            assert featureArray.length == numberFeatures;

            retval = retval.add(new ArrayRealVector(featureArray));
        }

        return retval.mapDivide(T);
    }

    public RealMatrix monteCarloGradient(RealMatrix x, PredictionProvider model) throws Exception {

        final int T = x.getRowDimension();
        final int F = x.getColumnDimension();

        final NormalDistribution N = new NormalDistribution(randomGenerator, 0.0, sigma);

        final RealMatrix[] U = new RealMatrix[ng];
        for (int i = 0; i < ng; i++) {
            U[i] = MatrixUtils.createRealMatrix(T, F); // Creates a matrix with T rows and F columns
        }

        for (int n = 0; n < ng; n++) {

            double sum = 0.0;
            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    U[n].setEntry(t, f, N.sample());
                    final double v = U[n].getEntry(t, f);
                    sum += v * v;
                }
            }

            final double L2norm = Math.sqrt(sum);

            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    // U[n][t][f] = (U[n][t][f]) / L2norm;
                    U[n].setEntry(t, f, U[n].getEntry(t, f) / L2norm);
                }
            }
        }

        final double[] diff = new double[ng];

        final List<PredictionInput> inputs = new LinkedList<PredictionInput>();

        for (int n = 0; n < ng; n++) {

            // dfs = f(x + u * sample) - f(x)

            final List<Feature> features2delta = new LinkedList<Feature>();

            for (int t = 0; t < T; t++) {

                final double[] feature3Array = new double[F];

                for (int f = 0; f < F; f++) {
                    feature3Array[f] = x.getEntry(t, f) + mu * U[n].getEntry(t, f);
                }

                final Feature feature3 = new Feature("xdelta" + t, Type.VECTOR, new Value(feature3Array));
                features2delta.add(feature3);
            }

            final PredictionInput inputDelta = new PredictionInput(features2delta);
            inputs.add(inputDelta);

        }

        final List<Feature> features2 = new LinkedList<Feature>();

        for (int t = 0; t < T; t++) {

            final double[] feature3Array = new double[F];

            for (int f = 0; f < F; f++) {
                feature3Array[f] = x.getEntry(t, f);
            }

            final Feature feature3 = new Feature("x" + t, Type.VECTOR, new Value(feature3Array));
            features2.add(feature3);
        }

        final PredictionInput input = new PredictionInput(features2);
        inputs.add(input);

        final CompletableFuture<List<PredictionOutput>> result = model.predictAsync(inputs);
        final List<PredictionOutput> results = result.get();

        // model prediction for original x
        final PredictionOutput fxPredictionOutput = results.get(results.size() - 1);
        final List<Output> fxs = fxPredictionOutput.getOutputs();
        final Output[] fx = fxs.toArray(new Output[0]);
        final double fxScore = fx[0].getScore();

        for (int i = 0; i < results.size() - 1; i++) {
            final PredictionOutput fxDeltaPredictionOutput = results.get(i);

            final List<Output> fxDeltas = fxDeltaPredictionOutput.getOutputs();

            final Output[] fxDelta = fxDeltas.toArray(new Output[0]);

            final double fxDeltaScore = fxDelta[0].getScore();

            diff[i] = fxDeltaScore - fxScore;
        }

        // g(t,j) = (T * F) / ng * sum(ng) (diff * U) / MU)

        final RealMatrix retval = MatrixUtils.createRealMatrix(T, F);

        final double mult = (double) (T * F) / ng;

        for (int t = 0; t < T; t++) {
            for (int f = 0; f < F; f++) {

                double gsum = 0.0;

                for (int n = 0; n < ng; n++) {
                    final double term = diff[n] * U[n].getEntry(t, f);
                    final double term2 = term / mu;
                    gsum += term2;
                }

                retval.setEntry(t, f, mult * gsum);
            }
        }

        return retval;

    }
}
