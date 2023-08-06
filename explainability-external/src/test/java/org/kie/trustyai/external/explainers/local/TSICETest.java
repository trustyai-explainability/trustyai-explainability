package org.kie.trustyai.external.explainers.local;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.local.TimeSeriesExplainer;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.external.interfaces.PassthroughPythonPredictionProvider;
import org.kie.trustyai.external.utils.PythonRuntimeManager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jep.SubInterpreter;
import jep.python.PyCallable;
import jep.python.PyObject;

class TSICETest {

    @Test
    @DisplayName("Test TSICE explainer construction only")
    void testConstruction() {
        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {
            final int nObs = 100;
            final Dataframe data = TimeseriesTest.createUnivariateDataframe(nObs, "timestamp", "sunspots");

            final int observationLength = 24;
            final int inputLength = 144;
            final int forecastHorizon = 12;
            final int nPerturbations = 250;

            final List<TSICEExplainer.AnalyseFeature> featuresToAnalyse =
                    List.of(TSICEExplainer.AnalyseFeature.MEAN, TSICEExplainer.AnalyseFeature.STD, TSICEExplainer.AnalyseFeature.MAX_VARIATION, TSICEExplainer.AnalyseFeature.TREND);

            final TSICEExplainer tsice = new TSICEExplainer.Builder()
                    .withInputLength(inputLength)
                    .withForecastLookahead(forecastHorizon)
                    .withNPerturbations(nPerturbations)
                    .withFeaturesToAnalyze(featuresToAnalyse)
                    .withExplanationWindowLength(observationLength)
                    .withExplanationWindowStart(36).build(sub, "192.168.0.47:8080", "tsforda", "v0.1.0");

            assertEquals("trustyaiexternal.algorithms.tsice", tsice.getNamespace());
            assertEquals("TSICEExplainer", tsice.getName());
        }
    }

    @Test
    @Disabled("This test is disabled because it requires a running model")
    void explainAsyncTest() throws ExecutionException, InterruptedException {

        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {
            final Dataframe sunspots = PrepareDatasets.getSunSpotsDataset();

            final int observationLength = 24;
            final int inputLength = 144;
            final int forecastHorizon = 12;
            final int nPerturbations = 250;

            final List<TSICEExplainer.AnalyseFeature> featuresToAnalyse =
                    List.of(TSICEExplainer.AnalyseFeature.MEAN, TSICEExplainer.AnalyseFeature.STD, TSICEExplainer.AnalyseFeature.MAX_VARIATION, TSICEExplainer.AnalyseFeature.TREND);

            final TimeSeriesExplainer<TSICEExplanation> tsice = new TSICEExplainer.Builder()
                    .withInputLength(inputLength)
                    .withForecastLookahead(forecastHorizon)
                    .withNPerturbations(nPerturbations)
                    .withFeaturesToAnalyze(featuresToAnalyse)
                    .withExplanationWindowLength(observationLength)
                    .withExplanationWindowStart(36).build(sub, "192.168.0.47:8080", "tsforda", "v0.1.0");

            // We import the model from Python by using the passthrough prediction provider
            sub.exec("from trustyaiexternal.models.forecaster import create_model");
            PyObject instance = (PyObject) ((PyCallable) sub.getValue("create_model")).call();
            final PredictionProvider model = new PassthroughPythonPredictionProvider((PyCallable) instance.getAttr("predict"));

            //            final TsFrame tsFrame = new TsFrame(sunspots.tail(inputLength), "month");

            // Request the explanation
            TSICEExplanation explanation = tsice.explainAsync(sunspots.tail(inputLength).asPredictions(), model).get();

            assertEquals(1, explanation.getDataX().size());
            assertEquals(inputLength, explanation.getDataX().get("sunspots").size());
            assertEquals(featuresToAnalyse.size(), explanation.getFeatureNames().size());
            assertEquals(featuresToAnalyse.size(), explanation.getFeatureValues().size());
            assertEquals(nPerturbations, explanation.getSignedImpact().size());
            assertEquals(nPerturbations, explanation.getTotalImpact().size());
            assertEquals(featuresToAnalyse.size(), explanation.getCurrentFeatureValues().size());
            assertEquals(nPerturbations, explanation.getPerturbations().size());
        }

    }

    @Test
    @DisplayName("Test TSICE explainer with a simple model")
    @Disabled("This test is disabled because it requires a running model")
    void simpleModel() throws ExecutionException, InterruptedException {

        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {

            final int nObs = 100;
            final Dataframe data = TimeseriesTest.createUnivariateDataframe(nObs, "timestamp", "sunspots");

            final int observationLength = 24;
            final int inputLength = 24;
            final int forecastHorizon = 4;
            final int nPerturbations = 10;

            final List<TSICEExplainer.AnalyseFeature> featuresToAnalyse = List.of(TSICEExplainer.AnalyseFeature.MEAN);

            final TimeSeriesExplainer<TSICEExplanation> tsice = new TSICEExplainer.Builder()
                    .withInputLength(inputLength)
                    .withForecastLookahead(forecastHorizon)
                    .withNPerturbations(nPerturbations)
                    .withFeaturesToAnalyze(featuresToAnalyse)
                    .withExplanationWindowLength(observationLength)
                    .withTimestampColumn("timestamp")
                    .build(sub, "192.168.0.47:8080", "forecaster", "v0.1.0");

            // Request the explanation
            TSICEExplanation explanation = tsice.explainAsync(data.tail(inputLength).asPredictions(), null).get();

            assertEquals(1, explanation.getDataX().size());
            assertEquals(nPerturbations, explanation.getDataX().get("sunspots").size());
            assertEquals(featuresToAnalyse.size(), explanation.getFeatureNames().size());
            assertEquals(featuresToAnalyse.size(), explanation.getFeatureValues().size());
            assertEquals(nPerturbations, explanation.getSignedImpact().size());
            assertEquals(nPerturbations, explanation.getTotalImpact().size());
            assertEquals(featuresToAnalyse.size(), explanation.getCurrentFeatureValues().size());
            assertEquals(nPerturbations, explanation.getPerturbations().size());
        }

    }
}
