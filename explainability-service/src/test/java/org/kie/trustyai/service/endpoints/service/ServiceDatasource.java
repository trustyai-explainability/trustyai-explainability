package org.kie.trustyai.service.endpoints.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;

import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
public class ServiceDatasource extends DataSource {

    public static final List<String> inputNames = List.of("age", "gender", "race");
    public static final List<String> outputNames = List.of("income");

    private Dataframe current;

    public ServiceDatasource() {
        generateRandomDataframe(10);
    }

    @Override
    public Dataframe getDataframe() throws DataframeCreateException {
        return this.current;
    }

    public void setDataframe(Dataframe dataframe) {
        this.current = dataframe;
    }

    public void generateRandomDataframe(int observations) {
        final List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random();
        for (int i = 0; i < observations; i++) {
            final List<Feature> featureList = List.of(
                    FeatureFactory.newNumericalFeature("age", random.nextInt(100)),
                    FeatureFactory.newNumericalFeature("gender", random.nextBoolean() ? 1 : 0),
                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
            final PredictionInput predictionInput = new PredictionInput(featureList);

            final List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            final PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        this.current = Dataframe.createFrom(predictions);
    }

}
