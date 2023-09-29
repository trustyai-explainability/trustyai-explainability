package org.kie.trustyai.service.mocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.arc.Priority;
import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
@Priority(1)
public class MockDatasource extends DataSource {

    private static final String MODEL_ID = "example1";
    @Inject
    Instance<MockMemoryStorage> storage;

    public MockDatasource() {
    }

    public Dataframe generateRandomDataframe(int observations) {
        return generateRandomDataframe(observations, 100);
    }

    public Dataframe generateRandomDataframe(int observations, int featureDiversity) {
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

    public Dataframe generateDataframeFromNormalDistributions(int observations, double mean, double stdDeviation) {
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

    public Dataframe generateRandomDataframeDrifted(int observations) {
        return generateRandomDataframeDrifted(observations, 100);
    }

    public Dataframe generateRandomDataframeDrifted(int observations, int featureDiversity) {
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

    public Dataframe generateRandomTextDataframe(int observations) {
        return generateRandomTextDataframe(observations, 0);
    }

    public Dataframe generateRandomTextDataframe(int observations, int seed) {
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

    public Metadata createMetadata(Dataframe dataframe) {
        final Metadata metadata = new Metadata();

        metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
        metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
        metadata.setObservations(dataframe.getRowDimension());

        return metadata;
    }

    public void reset() throws JsonProcessingException {
        this.empty();
        final Dataframe dataframe = generateRandomDataframe(1000);
        saveDataframe(dataframe, MODEL_ID);
        saveMetadata(createMetadata(dataframe), MODEL_ID);
    }

    public void empty() {
        storage.get().emptyStorage();
        this.knownModels.clear();
    }

    @Override
    public void saveDataframe(Dataframe dataframe, String modelId) throws InvalidSchemaException {
        super.saveDataframe(dataframe, modelId);
    }
}
