package org.kie.trustyai.explainability.local.tssaliency;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.*;

import static org.junit.jupiter.api.Assertions.*;

class TimeSeriesUtilsTest {

    public List<Prediction> readCSV(String filePath) {
        Random random = new Random();
        List<List<String>> columns = new ArrayList<>();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filePath);
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {

            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false; // Skip the header line
                    continue;
                }
                String[] values = line.split(",");
                for (int i = 2; i < values.length; i++) { // Skip the first column
                    if (columns.size() < (i - 2) + 1) {
                        columns.add(new ArrayList<>());
                    }
                    columns.get(i - 2).add(values[i]);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        int columnCount = columns.size();

        List<PredictionInput> predictionInputs = new ArrayList<>();
        for (int c = 0; c < columnCount; c++) {
            predictionInputs.add(new PredictionInput(columns.get(c).stream().map(col -> {
                return FeatureFactory.newNumericalFeature("t", Double.valueOf(col));
            }).collect(Collectors.toUnmodifiableList())));
        }

        List<Prediction> predictionList = new ArrayList<>();
        int nFeatures = predictionInputs.size();
        for (int i = 0; i < nFeatures; i++) {
            predictionList.add(new SimplePrediction(predictionInputs.get(i), new PredictionOutput(List.of(new Output("y-" + i, Type.NUMBER, new Value(random.nextDouble()), 1.0)))));
        }

        return predictionList;
    }

    private static List<Prediction> generateUnivariate(int timepoints) {
        final Random random = new Random();
        final List<PredictionInput> inputs = new ArrayList<>();
        final List<Feature> features = new ArrayList<>();
        for (int i = 0; i < timepoints; i++) {

            features.add(FeatureFactory.newNumericalFeature("x", random.nextDouble() * 10.0));

        }
        inputs.add(new PredictionInput(features));

        List<Prediction> predictionList = new ArrayList<>();
        for (PredictionInput input : inputs) {
            PredictionOutput po = new PredictionOutput(List.of(new Output("y-0", Type.NUMBER, new Value(random.nextDouble()), 1.0)));
            predictionList.add(new SimplePrediction(input, po));
        }
        return predictionList;
    }

    private static List<Prediction> generateMultivariate(int dimensions, int timepoints) {
        final Random random = new Random();
        final List<PredictionInput> inputs = new ArrayList<>();

        for (int f = 0; f < dimensions; f++) {
            final List<Feature> features = new ArrayList<>();
            for (int i = 0; i < timepoints; i++) {
                features.add(FeatureFactory.newNumericalFeature("x", random.nextDouble() * 10.0));
            }
            inputs.add(new PredictionInput(features));
        }

        List<Prediction> predictionList = new ArrayList<>();
        for (PredictionInput input : inputs) {
            PredictionOutput po = new PredictionOutput(List.of(new Output("y-0", Type.NUMBER, new Value(random.nextDouble()), 1.0)));
            predictionList.add(new SimplePrediction(input, po));
        }
        return predictionList;
    }

    @Test
    void testUnivariateToTSSaliency() {

        final int timepoints = 10;
        final List<Prediction> predictionList = generateUnivariate(timepoints);

        assertEquals(1, predictionList.size());
        assertEquals(timepoints, predictionList.get(0).getInput().getFeatures().size());

        List<Prediction> transformedPredictiopn = TimeSeriesUtils.toTSSaliencyTimeSeries(predictionList);

        assertEquals(1, transformedPredictiopn.size());
        assertEquals(timepoints, predictionList.get(0).getInput().getFeatures().size());
        assertTrue(transformedPredictiopn.get(0).getInput().getFeatures().stream().map(Feature::getType).allMatch(type -> type == Type.VECTOR));
        assertTrue(transformedPredictiopn.get(0).getInput().getFeatures().stream().map(Feature::getValue).map(Value::asVector).allMatch(vector -> vector.length == 1));
    }

    @Test
    void testBivariateToTSSaliency() {

        final int timepoints = 10;
        final int dimensions = 2;
        final List<Prediction> predictionList = generateMultivariate(dimensions, timepoints);

        assertEquals(dimensions, predictionList.size());
        assertEquals(timepoints, predictionList.get(0).getInput().getFeatures().size());

        List<Prediction> transformedPredictiopn = TimeSeriesUtils.toTSSaliencyTimeSeries(predictionList);

        assertEquals(1, transformedPredictiopn.size());
        assertEquals(timepoints, predictionList.get(0).getInput().getFeatures().size());
        assertEquals(timepoints, predictionList.get(1).getInput().getFeatures().size());
        assertTrue(transformedPredictiopn.get(0).getInput().getFeatures().stream().map(Feature::getType).allMatch(type -> type == Type.VECTOR));
        assertTrue(transformedPredictiopn.get(0).getInput().getFeatures().stream().map(Feature::getValue).map(Value::asVector).allMatch(vector -> vector.length == 2));
    }

    @Test
    void testUnivariateFromTSSaliency() {
        final int timepoints = 10;

        final List<Prediction> predictionList = TimeSeriesUtils.toTSSaliencyTimeSeries(generateUnivariate(timepoints));

        final List<PredictionInput> inputs = predictionList.stream().map(Prediction::getInput).collect(Collectors.toUnmodifiableList());

        final List<PredictionInput> transformedPredictions = TimeSeriesUtils.fromTSSaliencyTimeSeries(inputs);

        assertEquals(1, transformedPredictions.size());
        assertEquals(timepoints, transformedPredictions.get(0).getFeatures().size());
        assertTrue(transformedPredictions.get(0).getFeatures().stream().map(Feature::getType).allMatch(type -> type == Type.NUMBER));
    }

    @Test
    void testMultivariateFromTSSaliency() {
        final int timepoints = 10;
        final int dimensions = 2;
        final List<Prediction> predictionList = TimeSeriesUtils.toTSSaliencyTimeSeries(generateMultivariate(dimensions, timepoints));

        final List<PredictionInput> inputs = predictionList.stream().map(Prediction::getInput).collect(Collectors.toUnmodifiableList());

        final List<PredictionInput> transformedPredictions = TimeSeriesUtils.fromTSSaliencyTimeSeries(inputs);

        assertEquals(2, transformedPredictions.size());
        assertEquals(timepoints, transformedPredictions.get(0).getFeatures().size());
        assertTrue(transformedPredictions.get(0).getFeatures().stream().map(Feature::getType).allMatch(type -> type == Type.NUMBER));
    }

    @Test
    void testCSV() {

        final List<Prediction> inputs = readCSV("train_ts_x.csv");

        final int nFeatures = inputs.size();
        final int timepoints = inputs.get(0).getInput().getFeatures().size();

        assertEquals(11, nFeatures);
        assertEquals(4321, timepoints);

        final List<Prediction> transformedPredictions = TimeSeriesUtils.toTSSaliencyTimeSeries(inputs);

        assertEquals(1, transformedPredictions.size());
        assertEquals(4321, transformedPredictions.get(0).getInput().getFeatures().size());
        assertTrue(transformedPredictions.get(0).getInput().getFeatures().stream().map(Feature::getType).allMatch(type -> type == Type.VECTOR));
        assertTrue(transformedPredictions.get(0).getInput().getFeatures().stream().map(Feature::getValue).map(Value::asVector).mapToInt(a -> a.length).allMatch(size -> size == 11));
    }

}
