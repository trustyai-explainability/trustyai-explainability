package org.kie.trustyai.validation.explainability.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.metrics.fairness.group.DisparateImpactRatio;
import org.kie.trustyai.metrics.fairness.group.GroupStatisticalParityDifference;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SPDValidationTest {

    private Dataframe dataframe;

    @BeforeEach
    public void setUp() {
        final ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("validation/extdata/data.csv");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            // Skip the header
            List<String> lines = reader.lines().skip(1).collect(Collectors.toList());

            final List<Prediction> predictions = new ArrayList<>();
            for (String line : lines) {
                String[] values = line.split(",");
                final Feature age = FeatureFactory.newNumericalFeature("age", Integer.parseInt(values[0]));
                final Feature workclass = FeatureFactory.newNumericalFeature("workclass", Integer.parseInt(values[1]));
                final Feature fnlwgt = FeatureFactory.newNumericalFeature("fnlwgt", Integer.parseInt(values[2]));
                final Feature education = FeatureFactory.newNumericalFeature("education", Integer.parseInt(values[3]));
                final Feature education_num = FeatureFactory.newNumericalFeature("education-num", Integer.parseInt(values[4]));
                final Feature marital_status= FeatureFactory.newNumericalFeature("marital-status", Integer.parseInt(values[5]));
                final Feature occupation = FeatureFactory.newNumericalFeature("occupation", Integer.parseInt(values[6]));
                final Feature relationship = FeatureFactory.newNumericalFeature("relationship", Integer.parseInt(values[7]));
                final Feature race = FeatureFactory.newNumericalFeature("race", Integer.parseInt(values[8]));
                final Feature sex= FeatureFactory.newNumericalFeature("sex", Integer.parseInt(values[9]));
                final Feature capital_gain = FeatureFactory.newNumericalFeature("capital-gain", Integer.parseInt(values[10]));
                final Feature capital_loss= FeatureFactory.newNumericalFeature("capital-loss", Integer.parseInt(values[11]));
                final Feature hours_per_week = FeatureFactory.newNumericalFeature("hours-per-week", Integer.parseInt(values[12]));
                final Feature native_country= FeatureFactory.newNumericalFeature("native-country", Integer.parseInt(values[13]));
                final Output income = new Output("income", Type.NUMBER, new Value(Integer.parseInt(values[14])), 1d);

                final PredictionInput input = new PredictionInput(List.of(age, workclass, fnlwgt, education, education_num,
                        marital_status, occupation, relationship, race, sex, capital_gain, capital_loss, hours_per_week,
                        native_country));

                final PredictionOutput output = new PredictionOutput(List.of(income));
                predictions.add(new SimplePrediction(input, output));

            }
            dataframe = Dataframe.createFrom(predictions);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSPDValidationAIF() {
        final Predicate<Value> privilegedPredicate = value -> value.getUnderlyingObject().equals(1);
        final Predicate<Value> unprivilegedPredicate = value -> value.getUnderlyingObject().equals(0);
        final Dataframe privileged = dataframe.filterByColumnValue(9, privilegedPredicate);
        final Dataframe unprivileged = dataframe.filterByColumnValue(9, unprivilegedPredicate);

        final Output favourableOutcome = new Output("income", Type.NUMBER, new Value(0), 1d);
        final double spd = GroupStatisticalParityDifference.calculate(privileged, unprivileged, List.of(favourableOutcome));
        assertEquals(0.19643287553870947, spd, 1e-5);
    }

    @Test
    public void testSPDValidationDIR() {
        final Predicate<Value> privilegedPredicate = value -> value.getUnderlyingObject().equals(1);
        final Predicate<Value> unprivilegedPredicate = value -> value.getUnderlyingObject().equals(0);
        final Dataframe privileged = dataframe.filterByColumnValue(9, privilegedPredicate);
        final Dataframe unprivileged = dataframe.filterByColumnValue(9, unprivilegedPredicate);

        final Output favourableOutcome = new Output("income", Type.NUMBER, new Value(0), 1d);
        final double spd = DisparateImpactRatio.calculate(privileged, unprivileged, List.of(favourableOutcome));
        assertEquals(1.28, spd, 1e-2);
    }
}
