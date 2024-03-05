package org.kie.trustyai.service.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.model.domain.FeatureDomain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataframeGenerators {
    public static Dataframe generateRandomDataframe(int observations) {
        return generateRandomDataframe(observations, 100);
    }

    public static Dataframe generateRandomDataframe(int observations, int featureDiversity) {
        final List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random(0);
        for (int i = 0; i < observations; i++) {
            final List<Feature> featureList = List.of(
                    // guarantee feature diversity for age is min(observations, featureDiversity)
                    FeatureFactory.newNumericalFeature("age", i % featureDiversity),
                    FeatureFactory.newNumericalFeature("gender", random.nextBoolean() ? 1 : 0),
                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    public static Dataframe generateDataframeFromNormalDistributions(int observations, double mean, double stdDeviation) {
        final List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random(0);
        for (int i = 0; i < observations; i++) {
            double nextRand = random.nextGaussian();
            final List<Feature> featureList = List.of(
                    // guarantee feature diversity for age is min(observations, featureDiversity)
                    FeatureFactory.newNumericalFeature("f1", (nextRand * stdDeviation + mean)),
                    FeatureFactory.newNumericalFeature("f2", (nextRand * stdDeviation + 2 * mean)),
                    FeatureFactory.newNumericalFeature("f3", (nextRand * 2 * stdDeviation + mean)));
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    public static Dataframe generateRandomDataframeDrifted(int observations) {
        return generateRandomDataframeDrifted(observations, 100);
    }

    public static Dataframe generateRandomNColumnDataframe(int observations, int columns) {
        final List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random(0);
        for (int i = 0; i < observations; i++) {
            final List<Feature> featureList = IntStream.range(0, columns)
                    .mapToObj(idx -> FeatureFactory.newNumericalFeature("f" + idx, idx))
                    .collect(Collectors.toList());
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    public static Dataframe generatePositionalHintedDataframe(int rows, int columns) {
        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            int finalI = i;
            final List<Feature> featureList = IntStream.range(0, columns)
                    .mapToObj(idx -> FeatureFactory.newTextFeature("f" + idx, String.format("%d,%d", finalI, idx)))
                    .collect(Collectors.toList());
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("o0", Type.NUMBER, new Value(i), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    public static Dataframe generateRandomDataframeDrifted(int observations, int featureDiversity) {
        final List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random(0);
        for (int i = 0; i < observations; i++) {
            final List<Feature> featureList = List.of(
                    // guarantee feature diversity for age is min(observations, featureDiversity)
                    FeatureFactory.newNumericalFeature("age", (i % featureDiversity) + featureDiversity),
                    FeatureFactory.newNumericalFeature("gender", 0),
                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    public static Dataframe generateRandomTextDataframe(int observations) {
        return generateRandomTextDataframe(observations, 0);
    }

    public static Dataframe generateRandomTextDataframe(int observations, int seed) {
        final List<Prediction> predictions = new ArrayList<>();
        List<String> makes = List.of("Ford", "Chevy", "Dodge", "GMC", "Buick");
        List<String> colors = List.of("Red", "Blue", "White", "Black", "Purple", "Green", "Yellow");

        final Random random = new Random(seed);

        for (int i = 0; i < observations; i++) {
            final List<Feature> featureList = List.of(
                    // guarantee feature diversity for age is min(observations, featureDiversity)
                    FeatureFactory.newNumericalFeature("year", 1970 + i % 50),
                    FeatureFactory.newCategoricalFeature("make", makes.get(i % makes.size())),
                    FeatureFactory.newCategoricalFeature("color", colors.get(i % colors.size())));
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("value", Type.NUMBER, new Value(random.nextDouble() * 50), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    public static void roughEqualityCheck(Dataframe df1, Dataframe df2) {
        assertEquals(df1.getRowDimension(), df2.getRowDimension());
        assertEquals(df1.getColumnDimension(), df2.getColumnDimension());

        for (int col = 0; col < df1.getColumnDimension(); col++) {
            for (int row = 0; row < df1.getRowDimension(); row++) {
                assertEquals(df1.getValue(row, col), df2.getValue(row, col));

                FeatureDomain df1Domain = df1.getDomain(col);
                FeatureDomain df2Domain = df2.getDomain(col);

                assertEquals(df1Domain.getClass().getName(), df2Domain.getClass().getName());
                assertEquals(df1Domain.getCategories(), df2Domain.getCategories());
                assertEquals(df1Domain.getUpperBound(), df2Domain.getUpperBound());
                assertEquals(df1Domain.getLowerBound(), df2Domain.getLowerBound());
            }

            // inconsistent serialization of unmodifiable lists, so cast to array: https://github.com/fasterxml/jackson-databind/issues/2265
            assertArrayEquals(df1.getColumnNames().toArray(), df2.getColumnNames().toArray());
            assertArrayEquals(df1.getIds().toArray(), df2.getIds().toArray());
            assertArrayEquals(df1.getTags().toArray(), df2.getTags().toArray());
            assertArrayEquals(df1.getConstrained().toArray(), df2.getConstrained().toArray());
            assertArrayEquals(df1.getOutputsIndices().toArray(), df2.getOutputsIndices().toArray());
            assertArrayEquals(df1.getColumnTypes().toArray(), df2.getColumnTypes().toArray());
        }
    }

    public static void roughValueEqualityCheck(Dataframe df1, Dataframe df2) {
        assertEquals(df1.getRowDimension(), df2.getRowDimension());
        assertEquals(df1.getColumnDimension(), df2.getColumnDimension());

        for (int col = 0; col < df1.getColumnDimension(); col++) {
            for (int row = 0; row < df1.getRowDimension(); row++) {
                assertEquals(df1.getValue(row, col), df2.getValue(row, col));
            }
        }
    }
}
