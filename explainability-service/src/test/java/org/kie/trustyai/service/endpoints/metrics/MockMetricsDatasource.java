package org.kie.trustyai.service.endpoints.metrics;

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
public class MockMetricsDatasource extends DataSource {

    @Override
    public Dataframe getDataframe() throws DataframeCreateException {
        List<Prediction> predictions = new ArrayList<>();
        final Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            List<Feature> featureList = List.of(
                    FeatureFactory.newNumericalFeature("age", random.nextInt(100)),
                    FeatureFactory.newNumericalFeature("gender", random.nextBoolean() ? 1 : 0),
                    FeatureFactory.newNumericalFeature("race", random.nextBoolean() ? 1 : 0));
            PredictionInput predictionInput = new PredictionInput(featureList);

            List<Output> outputList = List.of(
                    new Output("income", Type.NUMBER, new Value(random.nextBoolean() ? 1 : 0), 1.0));
            PredictionOutput predictionOutput = new PredictionOutput(outputList);
            predictions.add(new SimplePrediction(predictionInput, predictionOutput));
        }
        return Dataframe.createFrom(predictions);
    }

    @Override
    public void appendDataframe(Dataframe dataframe) {

    }

}
