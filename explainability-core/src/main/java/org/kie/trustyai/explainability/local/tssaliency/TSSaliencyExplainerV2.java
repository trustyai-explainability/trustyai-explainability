package org.kie.trustyai.explainability.local.tssaliency;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.*;
import org.kie.trustyai.explainability.local.LocalExplainer;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.model.SaliencyResults.SourceExplainer;

public class TSSaliencyExplainerV2 implements LocalExplainer<SaliencyResults> {

    private static final double SIGMA = 10.0;
    private static final double MU = 0.01;
    final private int ng; // Number of samples for gradient estimation
    final private int nalpha; // Number of steps in convex path
    private RealVector baseValue; // check

    public TSSaliencyExplainerV2(double[] baseValue, int ng, int nalpha, int randomSeed) {
        this.baseValue = new ArrayRealVector(baseValue);
        this.ng = ng;
        this.nalpha = nalpha;
    }

    @Override
    public CompletableFuture<SaliencyResults> explainAsync(Prediction prediction, PredictionProvider model,
            Consumer<SaliencyResults> intermediateResultsConsumer) {

        try {
            final PredictionInput predictionInputs = prediction.getInput();

            final PredictionOutput predictionOutput = prediction.getOutput();

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
            final RealVector alpha = sequence.mapDivide(nalpha - 1d);

            // A nested array of doubles, where each element is a timepoint
            final RealMatrix x = MatrixUtils.createRealMatrix(T, F);
            for (int t = 0; t < T; t++) {
                final Feature feature = featuresArray[t];
                final Value value = feature.getValue();
                double[] elements = value.asVector();
                x.setRow(t, elements);
            }

            // Elements are initialised with zero anyway
            RealMatrix score = MatrixUtils.createRealMatrix(T, F);

            // Creating a matrix from baseValue with T identical rows
            final RealMatrix baseValueMatrix = new Array2DRowRealMatrix(T, baseValue.getDimension());
            for (int rowIndex = 0; rowIndex < T; rowIndex++) {
                baseValueMatrix.setRowVector(rowIndex, baseValue);
            }

            // for i 1 to n do
            for (int i = 0; i < nalpha; i++) {

                // Compute affine sample:
                // s = alpha(i) * X + (1 - alpha(i)) * (1(T) * transpose(b))

                final RealMatrix alphaX = x.scalarMultiply(alpha.getEntry(i)); // Multiply each element of x by alpha[i]

                final RealMatrix oneMinusAlphaBaseValue = baseValueMatrix.scalarMultiply(1.0 - alpha.getEntry(i)); // Multiply each element of baseValue by (1 - alpha[i])
                final RealMatrix s = alphaX.add(oneMinusAlphaBaseValue); // Add the results together

                // Compute Monte Carlo gradient (per time and feature dimension):

                // g = MC_GRADIENT(s; f; ng)
                // g is also an array of doubles, where each element is a timepoint
                final RealMatrix g = monteCarloGradient(s, model, ng);

                final RealMatrix gDivNalpha = g.scalarMultiply(1.0 / nalpha); // Divide g by nalpha
                score = score.add(gDivNalpha); // Add the result to score

                // n end for
            }

            // IG(t,j) = x(t,j) * SCORE(t,j)

            final Map<String, Saliency> saliencies = new HashMap<>();

            final SaliencyResults saliencyResults = new SaliencyResults(saliencies, SourceExplainer.TSSALIENCY);

            final RealMatrix fscore = score;

            final RealMatrix scoreResult = MatrixUtils.createRealMatrix(T, F);

            IntStream.range(0, T).forEach(t -> scoreResult.setRowVector(t, x.getRowVector(t).ebeMultiply(fscore.getRowVector(t))));

            final FeatureImportance featureImportance = new FeatureImportance(predictionInputs.getFeatures().get(0), scoreResult.getData(), 0.0);
            final List<FeatureImportance> featureImportances = new ArrayList<>(1);
            featureImportances.add(featureImportance);

            final List<Output> outputs = predictionOutput.getOutputs();
            final Output output = outputs.get(0);

            final Saliency saliency = new Saliency(output, featureImportances);
            saliencies.put("result", saliency);

            return CompletableFuture.completedFuture(saliencyResults);
        } catch (

        Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private RealVector calcBaseValue(Feature[] featuresArray, int T, int numberFeatures) {

        // 1/T sum(1..T) x(t, j)

        RealVector retval = new ArrayRealVector(numberFeatures);

        for (int t = 0; t < T; t++) {

            Feature feature = featuresArray[t];
            assert feature.getType() == Type.VECTOR;

            final Value featureValue = feature.getValue();
            double[] featureArray = featureValue.asVector();
            assert featureArray.length == numberFeatures;

            retval = retval.add(new ArrayRealVector(featureArray));
        }

        return retval.mapDivide(T);
    }

    private RealMatrix monteCarloGradient(RealMatrix x, PredictionProvider model, int ng) throws Exception {

        int T = x.getRowDimension();
        int F = x.getColumnDimension();

        // gradientSamples mu
        final NormalDistribution N = new NormalDistribution(0.0, SIGMA);

        // Sample ng independent data points from the unit sphere

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

            double L2norm = Math.sqrt(sum);

            for (int t = 0; t < T; t++) {
                for (int f = 0; f < F; f++) {
                    U[n].setEntry(t, f, U[n].getEntry(t, f) / L2norm);
                }
            }
        }

        double[] diff = new double[ng];

        final List<PredictionInput> inputs = new LinkedList<>();

        for (int n = 0; n < ng; n++) {

            // dfs = f(x + u * sample) - f(x)

            final List<Feature> features2delta = new LinkedList<>();

            for (int t = 0; t < T; t++) {

                double[] feature3Array = new double[F];

                for (int f = 0; f < F; f++) {
                    feature3Array[f] = x.getEntry(t, f) + MU * U[n].getEntry(t, f);
                }

                Feature feature3 = new Feature("xdelta" + t, Type.VECTOR, new Value(feature3Array));
                features2delta.add(feature3);
            }

            final PredictionInput inputDelta = new PredictionInput(features2delta);
            inputs.add(inputDelta);

            final List<Feature> features2 = new LinkedList<>();

            for (int t = 0; t < T; t++) {

                double[] feature3Array = new double[F];

                for (int f = 0; f < F; f++) {
                    feature3Array[f] = x.getEntry(t, f);
                }

                Feature feature3 = new Feature("x" + t, Type.VECTOR, new Value(feature3Array));
                features2.add(feature3);
            }

            PredictionInput input = new PredictionInput(features2);
            inputs.add(input);
        }

        final CompletableFuture<List<PredictionOutput>> result = model.predictAsync(inputs);
        final List<PredictionOutput> results = result.get();

        for (int i = 0; i < results.size(); i += 2) {
            final PredictionOutput fxDeltaPredictionOutput = results.get(i);
            final PredictionOutput fxPredictionOutput = results.get(i + 1);

            final List<Output> fxDeltas = fxDeltaPredictionOutput.getOutputs();
            final List<Output> fxs = fxPredictionOutput.getOutputs();

            final Output[] fxDelta = fxDeltas.toArray(new Output[0]);
            final Output[] fx = fxs.toArray(new Output[0]);

            final double fxDeltaScore = fxDelta[0].getScore();
            final double fxScore = fx[0].getScore();

            diff[i / 2] = fxDeltaScore - fxScore;
        }

        // g(t,j) = (T * F) / ng * sum(ng) (diff * U) / MU)

        final RealMatrix retval = MatrixUtils.createRealMatrix(T, F);

        double mult = (double) (T * F) / ng;

        for (int t = 0; t < T; t++) {
            for (int f = 0; f < F; f++) {

                double gsum = 0.0;

                for (int n = 0; n < ng; n++) {
                    double term = diff[n] * U[n].getEntry(t, f);
                    double term2 = term / MU;
                    gsum += term2;
                }

                retval.setEntry(t, f, mult * gsum);
            }
        }

        return retval;
    }
}
