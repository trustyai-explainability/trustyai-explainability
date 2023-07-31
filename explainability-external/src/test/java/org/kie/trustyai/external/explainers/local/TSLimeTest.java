package org.kie.trustyai.external.explainers.local;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.local.TimeSeriesExplainer;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.external.utils.PythonRuntimeManager;

import jep.SubInterpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TSLimeTest {

    @Test
    @DisplayName("Test TSLime explainer construction only")
    void testConstruction() {
        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {

            final int inputLength = 144;
            final int nPerturbations = 250;

            final TSLimeExplainer tsice = new TSLimeExplainer.Builder()
                    .withInputLength(inputLength)
                    .withNPerturbations(nPerturbations)
                    .build(sub, "192.168.0.47:8080", "tsforda", "v0.1.0");

            assertEquals("trustyaiexternal.algorithms.tslime", tsice.getNamespace());
            assertEquals("TSLimeExplainer", tsice.getName());
        }
    }

    @Test
    @DisplayName("Test TSLime explainer with a simple model")
    @Disabled("This test requires a running tsforda model")
    void simpleModel() throws ExecutionException, InterruptedException {

        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {

            final int nObs = 100;
            final Dataframe data = TimeseriesTest.createUnivariateDataframe(nObs, "timestamp", "sunspots");

            final int inputLength = 30;
            final int nPerturbations = 100;

            final TimeSeriesExplainer<TSLimeExplanation> tslime = new TSLimeExplainer.Builder()
                    .withInputLength(inputLength)
                    .withNPerturbations(nPerturbations)
                    .withTimestampColumn("timestamp")
                    .build(sub, "192.168.0.47:8080", "tsforda", "v0.1.0");

            // Request the explanation
            final TSLimeExplanation explanation = tslime.explainAsync(data.tail(inputLength).asPredictions(), null).get();

            //            assertEquals(inputLength * inputLength, explanation.getHistoryWeights().length);
            //            assertEquals(inputLength, explanation.getModelPrediction().length);
            //            assertEquals(inputLength * nPerturbations, explanation.getxPerturbations().length);
            //            assertEquals(inputLength, explanation.getSurrogatePrediction().length);
        }

    }

    // TODO: Test with multivariate data
}
