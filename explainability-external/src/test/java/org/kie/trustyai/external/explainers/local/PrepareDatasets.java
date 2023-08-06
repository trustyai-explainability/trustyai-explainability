package org.kie.trustyai.external.explainers.local;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.kie.trustyai.explainability.model.*;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class PrepareDatasets {

    public static Dataframe getAdultDataset() throws RuntimeException {
        List<Prediction> predictions = new ArrayList<>();
        String[] names = { "race", "sex", "Age (decade)=10", "Age (decade)=20", "Age (decade)=30", "Age (decade)=40", "Age (decade)=50", "Age (decade)=60", "Age (decade)=>=70", "Education Years=6",
                "Education Years=7", "Education Years=8", "Education Years=9", "Education Years=10", "Education Years=11", "Education Years=12", "Education Years=<6", "Education Years=>12" };
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(PrepareDatasets.class.getResourceAsStream("/python/adult.csv")));
                CSVReader csvReader = new CSVReader(reader)) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                final int ncols = line.length;
                final List<Feature> featureList = new ArrayList<>();
                final List<Output> outputList = new ArrayList<>();
                for (int col = 0; col < ncols; col++) {
                    if (col < ncols - 1) {
                        final Feature feature = FeatureFactory.newNumericalFeature(names[col], Double.parseDouble(line[col]));
                        featureList.add(feature);
                    } else {
                        final Output output = new Output("income", Type.NUMBER, new Value(Double.parseDouble(line[col])), 1.0);
                        outputList.add(output);
                    }
                }
                final Prediction prediction = new SimplePrediction(new PredictionInput(featureList), new PredictionOutput(outputList));
                predictions.add(prediction);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return Dataframe.createFrom(predictions);
    }

    public static Dataframe getSunSpotsDataset() throws RuntimeException {
        List<Prediction> predictions = new ArrayList<>();
        String[] names = { "month", "sunspots" };
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(PrepareDatasets.class.getResourceAsStream("/python/sunspots.csv")));
                CSVReader csvReader = new CSVReader(reader)) {
            String[] line;
            while ((line = csvReader.readNext()) != null) {

                final Feature date = FeatureFactory.newObjectFeature(names[0], line[0]);
                final Output sunspots = new Output(names[1], Type.NUMBER, new Value(Double.parseDouble(line[1])), 1.0);
                final List<Feature> inputs = List.of(date);
                final List<Output> outputs = List.of(sunspots);

                final Prediction prediction = new SimplePrediction(new PredictionInput(inputs), new PredictionOutput(outputs));
                predictions.add(prediction);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        }
        return Dataframe.createFrom(predictions);
    }
}
