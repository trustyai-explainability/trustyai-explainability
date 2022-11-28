package org.kie.trustyai.explainability.model;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.domain.FeatureDomain;
import org.kie.trustyai.explainability.model.domain.NumericalFeatureDomain;
import org.kie.trustyai.explainability.model.domain.ObjectFeatureDomain;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DataframeTest {

    private static final int N = 5000;

    public static Dataframe createTestDataframe() {

        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            final List<Feature> features = new ArrayList<>();
            features.add(FeatureFactory.newNumericalFeature("num-1", Math.random() * 100, NumericalFeatureDomain.create(0, 100)));
            features.add(FeatureFactory.newBooleanFeature("bool-2", new Random().nextBoolean(), ObjectFeatureDomain.create(Set.of(true, false))));
            features.add(FeatureFactory.newNumericalFeature("num-3", 1000.0 + Math.random() * 20.0));
            final PredictionInput input = new PredictionInput(features);

            final Output outputA = new Output("pred-1", Type.NUMBER, new Value(Math.random()), 0.0);
            final PredictionOutput output = new PredictionOutput(List.of(outputA));

            final Prediction prediction = new SimplePrediction(input, output);
            predictions.add(prediction);
        }
        return Dataframe.createFrom(predictions);
    }

    @Test
    void createFromPrediction() {

        final Feature featureA = FeatureFactory.newNumericalFeature("A", 10, NumericalFeatureDomain.create(-100, 100));
        final Feature featureB = FeatureFactory.newBooleanFeature("B", true);

        final PredictionInput input = new PredictionInput(List.of(featureA, featureB));

        final Output outputC = new Output("C", Type.BOOLEAN, new Value(false), 0.0);

        final PredictionOutput output = new PredictionOutput(List.of(outputC));

        final Prediction prediction = new SimplePrediction(input, output);

        final Dataframe df = Dataframe.createFrom(prediction);

        assertEquals(1, df.getRowDimension());
        assertEquals(3, df.getColumnDimension());
        assertFalse(df.getDomain(0).isEmpty());
        assertTrue(df.getDomain(1).isEmpty());
        assertTrue(df.getDomain(2).isEmpty());
        assertEquals(Type.NUMBER, df.getType(0));
        assertEquals(Type.BOOLEAN, df.getType(1));
        assertEquals(Type.BOOLEAN, df.getType(2));

    }


    @Test
    void getInputDataframe() {
        final Dataframe df = createTestDataframe();

        assertFalse(df.getOutputsIndices().isEmpty());

        final Dataframe inputDf = df.getInputDataframe();

        assertEquals(3, inputDf.getColumnDimension());
        assertTrue(inputDf.getOutputsIndices().isEmpty());
    }

    @Test
    void setColumnDomain() {
        final Dataframe df = createTestDataframe();
        assertTrue(df.getDomain(2).isEmpty());
        final FeatureDomain domain = NumericalFeatureDomain.create(-100, 1000);
        df.setColumnDomain(2, domain);
        assertFalse(df.getDomain(2).isEmpty());
        assertEquals(df.getDomain(2), domain);
    }

    @Test
    void isConstrained() {
        final Dataframe df = createTestDataframe();
        assertFalse(df.isConstrained(1));
        assertTrue(df.isConstrained(2));
        final FeatureDomain domain = NumericalFeatureDomain.create(-100, 1000);
        df.setColumnDomain(2, domain);
        assertFalse(df.isConstrained(2));
    }

    @Test
    void getType() {
        final Dataframe df = createTestDataframe();
        assertEquals(df.getType(0), Type.NUMBER);
        assertEquals(df.getType(1), Type.BOOLEAN);

        final List<Value> newValues = IntStream.range(0, N).mapToObj(i -> new Value(Math.random())).collect(Collectors.toList());
        df.setColumn(1, newValues);
        df.setType(1, Type.NUMBER);

        assertEquals(df.getType(1), Type.NUMBER);
    }

    @Test
    void getDomain() {
        final Dataframe df = createTestDataframe();
        assertFalse(df.getDomain(0).isEmpty());
        assertFalse(df.getDomain(1).isEmpty());
        assertTrue(df.getDomain(2).isEmpty());
        final FeatureDomain d1 = NumericalFeatureDomain.create(0, 100);
        final FeatureDomain d2 = ObjectFeatureDomain.create(Set.of(true, false));
        assertEquals(df.getDomain(0).getLowerBound(), d1.getLowerBound());
        assertEquals(df.getDomain(0).getUpperBound(), d1.getUpperBound());
        assertEquals(df.getDomain(1).getCategories(), d2.getCategories());
    }

    @Test
    void recalculateColumnDomain() {
        final Dataframe df = createTestDataframe();

        final FeatureDomain domain = df.calculateColumnDomain(0);

        assertTrue(df.getColumn(0).stream().allMatch(value -> {
            double d = value.asNumber();
            return d >= domain.getLowerBound() && d <= domain.getUpperBound();
        }));

        df.transformColumn(0, value -> new Value(value.asNumber() * 2.0));

        assertFalse(df.getColumn(0).stream().allMatch(value -> {
            double d = value.asNumber();
            return d >= domain.getLowerBound() && d <= domain.getUpperBound();
        }));

        final FeatureDomain domainNew = df.calculateColumnDomain(0);

        assertTrue(df.getColumn(0).stream().allMatch(value -> {
            double d = value.asNumber();
            return d >= domainNew.getLowerBound() && d <= domainNew.getUpperBound();
        }));
    }

    @Test
    void getConstrained() {
        final Dataframe df = createTestDataframe();
        assertEquals(List.of(2, 3), df.getConstrained());

        final FeatureDomain domain = NumericalFeatureDomain.create(-100, 1000);
        df.setColumnDomain(2, domain);
        assertEquals(List.of(3), df.getConstrained());
    }

    @Test
    void getRowDimension() {
        final Dataframe df = createTestDataframe();

        assertEquals(N, df.getRowDimension());
    }

    @Test
    void getColumnDimension() {
        final Dataframe df = createTestDataframe();

        assertEquals(4, df.getColumnDimension());

        df.dropColumn(0);

        assertEquals(3, df.getColumnDimension());

        df.dropColumns(0, 1, 2);

        assertEquals(0, df.getColumnDimension());

    }

    @Test
    void getColumn() {
        final Dataframe df = createTestDataframe();

        final List<Value> column = df.getColumn(0);

        assertEquals(N, column.size());
        assertTrue(column.stream().allMatch(value -> value.getUnderlyingObject() instanceof Double));
    }

    @Test
    void getInputsIndices() {
        final Dataframe df = createTestDataframe();

        assertEquals(List.of(0, 1, 2), df.getInputsIndices());

        df.dropColumn(1);

        assertEquals(List.of(0, 1), df.getInputsIndices());

        df.dropColumns(2); // output column

        assertEquals(List.of(0, 1), df.getInputsIndices());
    }

    @Test
    void getOutputsIndices() {
        final Dataframe df = createTestDataframe();

        assertEquals(List.of(3), df.getOutputsIndices());

        df.dropColumn(1);

        assertEquals(List.of(2), df.getOutputsIndices());

        df.dropColumns(2); // output column

        assertTrue(df.getOutputsIndices().isEmpty());
    }


    @Test
    void copy() {
        final Dataframe original = createTestDataframe();
        final Dataframe copy = original.copy();

        assertEquals(original.getValue(2500, 0), copy.getValue(2500, 0));
        assertEquals(original.getDomain(0), copy.getDomain(0));

        copy.setValue(2500, 0, new Value(-999.0));
        copy.setColumnDomain(0, NumericalFeatureDomain.create(-9999, -999));

        assertNotEquals(original.getValue(2500, 0), copy.getValue(2500, 0));
        assertNotEquals(original.getDomain(0), copy.getDomain(0));

    }


    @Test
    void filterByRowIndex() {
        final Dataframe dataframe = createTestDataframe();

        final Dataframe top = dataframe.filterByRowIndex(IntStream.range(0, 100).boxed().collect(Collectors.toList()));

        assertEquals(dataframe.getValue(50, 0), top.getValue(50, 0));
        assertEquals(100, top.getRowDimension());

        top.setValue(50, 0, new Value(-999));
        assertNotEquals(dataframe.getValue(50, 0), top.getValue(50, 0));
    }

    @Test
    void filterByColumnValue() {
        final Dataframe dataframe = createTestDataframe();

        assertFalse(dataframe.getColumn(1).stream().map(value -> (Boolean) value.getUnderlyingObject()).allMatch(value -> !value));

        final Dataframe filtered = dataframe.filterByColumnValue(1, value -> value.getUnderlyingObject().equals(false));

        assertTrue(filtered.getRowDimension() < dataframe.getRowDimension());

        assertTrue(filtered.getColumn(1).stream().map(value -> (Boolean) value.getUnderlyingObject()).allMatch(value -> !value));

    }

    @Test
    void transformColumn() {
        final Dataframe df = createTestDataframe();

        final double sumBefore = df.getColumn(0).stream().mapToDouble(Value::asNumber).sum();

        df.transformColumn(0, value -> new Value(2.0 * value.asNumber()));

        final double sumAfter = df.getColumn(0).stream().mapToDouble(Value::asNumber).sum();

        assertEquals(sumAfter, 2.0 * sumBefore);
    }

    @Test
    void transformRow() {

        final Dataframe df = createTestDataframe();

        List<Value> original = df.getRow(100);

        df.transformRow(100, value -> new Value(value.asNumber() / 2.0));

        assertEquals(original.stream().mapToDouble(Value::asNumber).sum() / 2.0, df.getRow(100).stream().mapToDouble(Value::asNumber).sum());
    }


    @Test
    void sortRowsByColumn() {
        final Dataframe original = createTestDataframe();
        final Dataframe toSort = original.copy();
        final int column = 0;

        toSort.sortRowsByColumn(column, Comparator.comparingDouble(Value::asNumber));

        assertNotEquals(original.getColumn(column), toSort.getColumn(column));
        assertEquals(5000, toSort.getRowDimension());
        assertEquals(4, toSort.getColumnDimension());
        assertEquals(original.getColumn(column).stream().sorted(Comparator.comparingDouble(Value::asNumber)).collect(Collectors.toList()), toSort.getColumn(column));
    }

    @Test
    void dropColumn() {
        final Dataframe df = createTestDataframe();

        assertEquals(4, df.getColumnDimension());

        df.dropColumn(2);

        assertEquals(3, df.getColumnDimension());

        assertEquals(List.of("num-1", "bool-2", "pred-1"), df.getColumnNames());
    }

    @Test
    void reduceRow() {
        final Dataframe df = createTestDataframe();

        Function<List<Value>, Value> fn = (List<Value> values) -> new Value(values.stream().mapToDouble(Value::asNumber).sum() * 2.0);

        Value value = df.reduceRow(100, fn);

        assertEquals(fn.apply(df.getRow(100)), value);
    }

    @Test
    void reduceRows() {
        final Dataframe df = createTestDataframe();

        Function<List<Value>, Value> fn = (List<Value> values) -> new Value(values.stream().mapToDouble(Value::asNumber).sum() * 2.0);

        List<Value> value = df.reduceRows(fn);

        assertEquals(fn.apply(df.getRow(100)), value.get(100));

    }

    @Test
    void addColumn() {
        final Dataframe df = createTestDataframe();

        df.addColumn("new", Type.NUMBER, IntStream.range(0, N).mapToObj(Value::new).collect(Collectors.toList()));

        assertEquals(5, df.getColumnDimension());
    }

    @Test
    void dropColumns() {
        final Dataframe df = createTestDataframe();

        assertEquals(4, df.getColumnDimension());

        df.dropColumns(1, 2);

        assertEquals(2, df.getColumnDimension());

        assertEquals(List.of("num-1", "pred-1"), df.getColumnNames());

    }

    @Test
    void getColumnNames() {
        final Dataframe df = createTestDataframe();

        assertEquals(List.of("num-1", "bool-2", "num-3", "pred-1"), df.getColumnNames());

        df.addColumn("pred-2", Type.NUMBER, IntStream.range(0, N).mapToObj(Value::new).collect(Collectors.toList()));

        assertEquals(List.of("num-1", "bool-2", "num-3", "pred-1", "pred-2"), df.getColumnNames());

    }

    @Test
    void getOutputsCount() {
        final Dataframe df = createTestDataframe();

        assertEquals(1, df.getOutputsCount());

        df.dropColumn(3);

        assertEquals(0, df.getOutputsCount());
    }

    @Test
    void getInputNames() {
        final Dataframe df = createTestDataframe();

        assertEquals(List.of("num-1", "bool-2", "num-3"), df.getInputNames());
    }

    @Test
    void getOutputNames() {
        final Dataframe df = createTestDataframe();

        assertEquals(List.of("pred-1"), df.getOutputNames());
    }

    @Test
    void testGetColumnNames() {
        final Dataframe df = createTestDataframe();

        assertEquals(List.of("bool-2", "num-3"), df.getColumnNames(List.of(1, 2)));

    }

}