package org.kie.trustyai.service.mocks;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.utils.DataframeGenerators;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.Mock;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

@Mock
@Alternative
@ApplicationScoped
@Priority(1)
public class MockDatasource extends DataSource {

    private static final String MODEL_ID = "example1";

    public MockDatasource() {
    }

    public StorageMetadata createMetadata(Dataframe dataframe) {
        final StorageMetadata storageMetadata = new StorageMetadata();

        storageMetadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
        storageMetadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
        storageMetadata.setObservations(dataframe.getRowDimension());
        storageMetadata.setModelId(dataframe.getId());

        return storageMetadata;
    }
    //
    //    public Dataframe generateRandomDataframe(int observations) {
    //        return generateRandomDataframe(observations, 100, false);
    //    }
    //
    //    public Dataframe generateRandomSyntheticDataframe(int observations) {
    //        return generateRandomDataframe(observations, 100, true);
    //    }
    //
    //    /**
    //     * Generate a random dataframe with a given number of observations and feature diversity.
    //     * The data can also be labelled as synthetic or non-synthetic.
    //     * @param observations the number of observations
    //     * @param featureDiversity the number of unique values for the feature "age"
    //     * @param synthetic whether the data is synthetic or not
    //     * @return a random {@link Dataframe}
    //     */
    //    public Dataframe generateRandomDataframe(int observations, int featureDiversity, boolean synthetic) {
    //        final List<Prediction> predictions = new ArrayList<>();
    //        final Random random = new Random(0);
    //        for (int i = 0; i < observations; i++) {
    //            final List<Feature> featureList = List.of(
    //                    // guarantee feature diversity for age is min(observations, featureDiversity)
    //                    FeatureFactory.newNumericalFeature("age", i % featureDiversity),
    //                    FeatureFactory.newNumericalFeature("gender", random.nextBoolean() ? 1 : 0),
    //                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
    //            final PredictionInput predictionInput = new PredictionInput(featureList);
    //
    //            final List<Output> outputList = List.of(
    //                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
    //            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
    //            if (synthetic) {
    //                final PredictionMetadata predictionMetadata = new PredictionMetadata(UUID.randomUUID().toString(),
    //                        LocalDateTime.now(), Dataframe.InternalTags.SYNTHETIC.get());
    //                predictions.add(new SimplePrediction(predictionInput, predictionOutput, predictionMetadata));
    //            } else {
    //                predictions.add(new SimplePrediction(predictionInput, predictionOutput));
    //            }
    //
    //        }
    //        return Dataframe.createFrom(predictions);
    //    }
    //
    //    public Dataframe generateDataframeFromNormalDistributions(int observations, double mean, double stdDeviation) {
    //        final List<Prediction> predictions = new ArrayList<>();
    //        final Random random = new Random(0);
    //        for (int i = 0; i < observations; i++) {
    //            double nextRand = random.nextGaussian();
    //            final List<Feature> featureList = List.of(
    //                    // guarantee feature diversity for age is min(observations, featureDiversity)
    //                    FeatureFactory.newNumericalFeature("f1", (nextRand * stdDeviation + mean)),
    //                    FeatureFactory.newNumericalFeature("f2", (nextRand * stdDeviation + 2 * mean)),
    //                    FeatureFactory.newNumericalFeature("f3", (nextRand * 2 * stdDeviation + mean)));
    //            final PredictionInput predictionInput = new PredictionInput(featureList);
    //
    //            final List<Output> outputList = List.of(
    //                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
    //            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
    //            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
    //        }
    //        return Dataframe.createFrom(predictions);
    //    }
    //
    //    public Dataframe generateRandomDataframeDrifted(int observations) {
    //        return generateRandomDataframeDrifted(observations, 100);
    //    }
    //
    //    public Dataframe generateRandomNColumnDataframe(int observations, int columns) {
    //        final List<Prediction> predictions = new ArrayList<>();
    //        final Random random = new Random(0);
    //        for (int i = 0; i < observations; i++) {
    //            final List<Feature> featureList = IntStream.range(0, columns)
    //                    .mapToObj(idx -> FeatureFactory.newNumericalFeature("f" + idx, idx))
    //                    .collect(Collectors.toList());
    //            final PredictionInput predictionInput = new PredictionInput(featureList);
    //
    //            final List<Output> outputList = List.of(
    //                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
    //            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
    //            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
    //        }
    //        return Dataframe.createFrom(predictions);
    //    }
    //
    //    public Dataframe generateRandomDataframeDrifted(int observations, int featureDiversity) {
    //        final List<Prediction> predictions = new ArrayList<>();
    //        final Random random = new Random(0);
    //        for (int i = 0; i < observations; i++) {
    //            final List<Feature> featureList = List.of(
    //                    // guarantee feature diversity for age is min(observations, featureDiversity)
    //                    FeatureFactory.newNumericalFeature("age", (i % featureDiversity) + featureDiversity),
    //                    FeatureFactory.newNumericalFeature("gender", 0),
    //                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
    //            final PredictionInput predictionInput = new PredictionInput(featureList);
    //
    //            final List<Output> outputList = List.of(
    //                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
    //            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
    //            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
    //        }
    //        return Dataframe.createFrom(predictions);
    //    }
    //
    //    public Dataframe generateRandomTextDataframe(int observations) {
    //        return generateRandomTextDataframe(observations, 0);
    //    }
    //
    //    public Dataframe generateRandomTextDataframe(int observations, int seed) {
    //        final List<Prediction> predictions = new ArrayList<>();
    //        List<String> makes = List.of("Ford", "Chevy", "Dodge", "GMC", "Buick");
    //        List<String> colors = List.of("Red", "Blue", "White", "Black", "Purple", "Green", "Yellow");
    //
    //        final Random random = new Random(seed);
    //
    //        for (int i = 0; i < observations; i++) {
    //            final List<Feature> featureList = List.of(
    //                    // guarantee feature diversity for age is min(observations, featureDiversity)
    //                    FeatureFactory.newNumericalFeature("year", 1970 + i % 50),
    //                    FeatureFactory.newCategoricalFeature("make", makes.get(i % makes.size())),
    //                    FeatureFactory.newCategoricalFeature("color", colors.get(i % colors.size())));
    //            final PredictionInput predictionInput = new PredictionInput(featureList);
    //
    //            final List<Output> outputList = List.of(
    //                    new Output("value", Type.NUMBER, new Value(random.nextDouble() * 50), 1.0));
    //            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
    //            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
    //        }
    //        return Dataframe.createFrom(predictions);
    //    }

    public void reset() throws JsonProcessingException {
        this.knownModels.clear();
    }

    public void populate() {
        final Dataframe dataframe = DataframeGenerators.generateRandomDataframe(1000);
        saveDataframe(dataframe, MODEL_ID);
        saveMetadata(createMetadata(dataframe), MODEL_ID);

    }

    @Override
    public void saveDataframe(Dataframe dataframe, String modelId) throws InvalidSchemaException {
        super.saveDataframe(dataframe, modelId);
    }
}
