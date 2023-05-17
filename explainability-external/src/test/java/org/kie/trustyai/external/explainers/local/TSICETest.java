package org.kie.trustyai.external.explainers.local;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.external.interfaces.PassthroughPythonPredictionProvider;
import org.kie.trustyai.external.interfaces.TimeSeriesExplainer;
import org.kie.trustyai.external.interfaces.TsFrame;
import org.kie.trustyai.external.utils.PrepareDatasets;
import org.kie.trustyai.external.utils.PythonWrapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jep.SubInterpreter;
import jep.python.PyCallable;
import jep.python.PyObject;

class TSICETest {

    @Test
    void explainAsync() throws ExecutionException, InterruptedException {

        try (SubInterpreter sub = PythonWrapper.INSTANCE.getSubInterpreter()) {
            final Dataframe sunspots = PrepareDatasets.getSunSpotsDataset();

            final int observationLength = 24;
            final int inputLength = 144;
            final int forecastHorizon = 12;
            final int nPerturbations = 250;

            final List<TSICE.AnalyseFeature> featuresToAnalyse = List.of(TSICE.AnalyseFeature.MEAN, TSICE.AnalyseFeature.STD, TSICE.AnalyseFeature.MAX_VARIATION, TSICE.AnalyseFeature.TREND);

            final TimeSeriesExplainer<TSICEExplanation> tsice = new TSICE.Builder()
                    .withInputLength(inputLength)
                    .withForecastLookahead(forecastHorizon)
                    .withNPerturbations(nPerturbations)
                    .withFeaturesToAnalyze(featuresToAnalyse)
                    .withExplanationWindowLength(observationLength)
                    .withExplanationWindowStart(36).build(sub, "", "");

            // We import the model from Python by using the passthrough prediction provider
            sub.exec("from trustyaiexternal.models.forecaster import create_model");
            PyObject instance = (PyObject) ((PyCallable) sub.getValue("create_model")).call();
            final PredictionProvider model = new PassthroughPythonPredictionProvider((PyCallable) instance.getAttr("predict"));

            final TsFrame tsFrame = new TsFrame(sunspots.tail(inputLength), "month");

            // Request the explanation
            TSICEExplanation explanation = tsice.explainAsync(tsFrame, model).get();

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
}