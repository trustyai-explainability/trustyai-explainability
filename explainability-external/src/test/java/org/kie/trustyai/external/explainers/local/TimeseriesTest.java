package org.kie.trustyai.external.explainers.local;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.external.interfaces.TsFrame;
import org.kie.trustyai.external.utils.PythonRuntimeManager;

import jep.SubInterpreter;
import jep.python.PyCallable;
import jep.python.PyObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TimeseriesTest {

    public static Dataframe createUnivariateDataframe(int nObs, String timestampName, String featureName) {
        final Random random = new Random();
        final List<PredictionInput> inputs = new ArrayList<>();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final LocalDate startDate = LocalDate.of(2020, 1, 1);

        for (int i = 0; i < nObs; i++) {
            inputs.add(new PredictionInput(List.of(
                    FeatureFactory.newTextFeature(timestampName, startDate.plusDays(i).format(formatter)),
                    FeatureFactory.newNumericalFeature(featureName, random.nextInt(100)))));
        }

        return Dataframe.createFromInputs(inputs);
    }

    private static List<String> getColumnNames(PyObject tsFrame) {
        PyObject columns = ((PyObject) tsFrame.getAttr("columns"));
        PyObject values = ((PyObject) columns.getAttr("values"));
        PyCallable tolist = ((PyCallable) values.getAttr("tolist"));
        return (List<String>) tolist.call();
    }

    public static Dataframe createMultivariateDataframe(int nObs, String timestampName, String featureName, int dimensions) {
        final Random random = new Random();
        final List<PredictionInput> inputs = new ArrayList<>();
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        final LocalDate startDate = LocalDate.of(2020, 1, 1);

        for (int i = 0; i < nObs; i++) {
            final List<Feature> features = new ArrayList<>();
            features.add(FeatureFactory.newTextFeature(timestampName, startDate.plusDays(i).format(formatter)));
            for (int d = 0; d < dimensions; d++) {
                features.add(FeatureFactory.newNumericalFeature(featureName + "-" + (d + 1), random.nextInt(100)));
            }
            inputs.add(new PredictionInput(features));
        }

        return Dataframe.createFromInputs(inputs);
    }

    @Test
    @DisplayName("Convert an univariate dataframe to a Python tsframe")
    void toTsFrame() {
        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {

            final int nObs = 20;
            final Dataframe data = createUnivariateDataframe(nObs, "timestamp", "x");

            assertEquals(nObs, data.getRowDimension());
            assertEquals(2, data.getColumnDimension());

            final TsFrame tsFrame = new TsFrame(data, "timestamp");
            final PyObject pythonTsFrame = tsFrame.getTsFrame(sub);

            final List<String> names = getColumnNames(pythonTsFrame);

            assertEquals(List.of("x"), names);
        }
    }

    @Test
    @DisplayName("Convert an univariate dataframe to a Python tsframe with incorrect timestamp name")
    void toTsFrameMissingColumn() {
        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {

            final int nObs = 100;
            final Dataframe data = createUnivariateDataframe(nObs, "timestamp", "x");

            assertEquals(nObs, data.getRowDimension());
            assertEquals(2, data.getColumnDimension());

            final TsFrame tsFrame = new TsFrame(data, "month");

            Exception exception = assertThrows(IllegalArgumentException.class, () -> tsFrame.getTsFrame(sub));
            assertEquals("The dataframe does not contain the month column", exception.getMessage());
        }
    }

    @Test
    @DisplayName("Convert an multivariate dataframe to a Python tsframe")
    void toTsFrameMultivariate() {
        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {

            final int nObs = 30;
            final int dimensions = 20;
            final Dataframe data = createMultivariateDataframe(nObs, "timestamp", "x", dimensions);

            assertEquals(nObs, data.getRowDimension());
            assertEquals(dimensions + 1, data.getColumnDimension());

            final TsFrame tsFrame = new TsFrame(data, "timestamp");
            final PyObject pythonTsFrame = tsFrame.getTsFrame(sub);

            final List<String> names = getColumnNames(pythonTsFrame);

            assertEquals(IntStream.range(1, dimensions + 1).mapToObj(n -> "x-" + n).collect(Collectors.toList()), names);
        }
    }

    @Test
    @DisplayName("Convert an multivariate dataframe to a Python tsframe with incorrect timestamp name")
    void toTsFrameMissingColumnMultivariate() {
        try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {

            final int nObs = 100;
            final Dataframe data = createUnivariateDataframe(nObs, "timestamp", "x");

            assertEquals(nObs, data.getRowDimension());
            assertEquals(2, data.getColumnDimension());

            final TsFrame tsFrame = new TsFrame(data, "month");

            Exception exception = assertThrows(IllegalArgumentException.class, () -> tsFrame.getTsFrame(sub));
            assertEquals("The dataframe does not contain the month column", exception.getMessage());
        }
    }
}
