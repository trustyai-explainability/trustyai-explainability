package org.kie.trustyai.explainability.model;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.domain.EmptyFeatureDomain;
import org.kie.trustyai.explainability.model.domain.FeatureDomain;
import org.kie.trustyai.explainability.model.domain.NumericalFeatureDomain;
import org.kie.trustyai.explainability.model.domain.ObjectFeatureDomain;

public class Dataframe {

    private final List<List<Value>> data;
    private final Metadata metadata;

    private Dataframe() {
        this.data = new ArrayList<>(new ArrayList<>());
        this.metadata = new Metadata();
    }

    private Dataframe(List<List<Value>> data, Metadata metadata) {
        this.data = new ArrayList<>(data);
        this.metadata = metadata;
    }

    /**
     * Create a dataframe from a single @link{Prediction}
     *
     * @param prediction The original @link{Prediction}
     * @return A @link{Dataframe}
     */
    public static Dataframe createFrom(Prediction prediction) {
        final Dataframe df = new Dataframe();

        // Process inputs metadata
        for (Feature feature : prediction.getInput().getFeatures()) {
            df.metadata.names.add(feature.getName());
            df.metadata.types.add(feature.getType());
            df.metadata.constrained.add(feature.isConstrained());
            df.metadata.domains.add(feature.getDomain());
            df.metadata.inputs.add(true);
            df.data.add(new ArrayList<>());
        }
        // Process outputs metadata
        for (Output output : prediction.getOutput().getOutputs()) {
            df.metadata.names.add(output.getName());
            df.metadata.types.add(output.getType());
            df.metadata.constrained.add(true);
            df.metadata.domains.add(EmptyFeatureDomain.create());
            df.metadata.inputs.add(false);
            df.data.add(new ArrayList<>());
        }

        // Copy data
        df.addPrediction(prediction);

        return df;
    }

    /**
     * Create a dataframe from a @link{Dataset}
     *
     * @param dataset The original @link{Dataset}
     * @return A @link{Dataframe}
     */
    public static Dataframe createFrom(Dataset dataset) {
        return createFrom(dataset.getData());
    }

    /**
     * Create a dataframe from a list of @link{Prediction}
     *
     * @param predictions The original @link{Prediction} list
     * @return A @link{Dataframe}
     */
    public static Dataframe createFrom(List<Prediction> predictions) {
        final Prediction prediction = predictions.get(0);
        final Dataframe df = Dataframe.createFrom(prediction);

        if (predictions.size() > 1) {
            final List<Prediction> rest = predictions.subList(1, predictions.size());
            df.addPredictions(rest);
        }

        return df;
    }

    public static Dataframe createFromInputs(List<PredictionInput> inputs) {
        final Dataframe df = new Dataframe();

        // Process inputs metadata
        for (Feature feature : inputs.get(0).getFeatures()) {
            df.metadata.names.add(feature.getName());
            df.metadata.types.add(feature.getType());
            df.metadata.constrained.add(feature.isConstrained());
            df.metadata.domains.add(feature.getDomain());
            df.metadata.inputs.add(true);
            df.data.add(new ArrayList<>());
        }

        final int inputsSize = df.getInputsCount();

        IntStream.range(0, inputs.size()).forEach(i -> {
            final List<Feature> currentInputs = inputs.get(i).getFeatures();
            // Copy data
            for (int col = 0; col < inputsSize; col++) {
                df.data.get(col).add(currentInputs.get(col).getValue());
            }
        });

        return df;
    }

    public static Dataframe createFrom(List<PredictionInput> inputs, List<PredictionOutput> outputs) {
        if (inputs.size() != outputs.size()) {
            throw new IllegalArgumentException("Inputs and outputs have different dimensions (" + inputs.size() + " and " + outputs.size() + ")");
        }
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Must supply at least one input and output.");
        }

        final List<Prediction> predictions = IntStream.range(0, inputs.size()).mapToObj(i -> new SimplePrediction(inputs.get(i), outputs.get(i))).collect(Collectors.toList());

        final Prediction prediction = predictions.get(0);
        final Dataframe df = Dataframe.createFrom(prediction);

        final List<Prediction> rest = predictions.subList(1, predictions.size());
        df.addPredictions(rest);

        return df;
    }

    /**
     * Return a copy of the dataframe containing only input columns
     *
     * @return A @link{Dataframe}
     */
    public Dataframe getInputDataframe() {
        final Dataframe df = this.copy();
        df.dropColumns(getOutputsIndices());
        return df;
    }

    /**
     * Return a copy of the dataframe containing only output columns
     *
     * @return A @link{Dataframe}
     */
    public Dataframe getOutputDataframe() {
        final Dataframe df = this.copy();
        df.dropColumns(getInputsIndices());
        return df;
    }

    /**
     * Add a single prediction (as a row) to the @link{Dataframe}
     *
     * @param prediction The @link{Prediction} to add.
     */
    public void addPrediction(Prediction prediction) {
        addPredictions(List.of(prediction));
    }

    /**
     * Add a {@link List} of predictions to the {@link Dataframe}
     *
     * @param predictions The {@link List} of {@link Prediction} to add.
     */
    public void addPredictions(List<Prediction> predictions) {
        final Prediction prediction = predictions.get(0);
        final List<Feature> inputs = prediction.getInput().getFeatures();
        final List<Output> outputs = prediction.getOutput().getOutputs();

        // Validate schema
        if (!getInputNames().equals(inputs.stream().map(Feature::getName).collect(Collectors.toList()))) {
            throw new IllegalArgumentException("Prediction inputs do not match dataframe inputs");
        }
        if (!getOutputNames().equals(outputs.stream().map(Output::getName).collect(Collectors.toList()))) {
            throw new IllegalArgumentException("Prediction outputs do not match dataframe inputs");
        }

        final int inputsSize = getInputsCount();

        IntStream.range(0, predictions.size()).forEach(i -> {
            final List<Feature> currentInputs = predictions.get(i).getInput().getFeatures();
            final List<Output> currentOutputs = predictions.get(i).getOutput().getOutputs();
            // Copy data
            for (int col = 0; col < inputsSize; col++) {
                data.get(col).add(currentInputs.get(col).getValue());
            }
            final int nFeatures = getColumnDimension();
            for (int col = inputsSize; col < nFeatures; col++) {
                data.get(col).add(currentOutputs.get(col - inputsSize).getValue());
            }
        });
    }

    /**
     * Get a {@link List} of column names marked as inputs.
     *
     * @return A {@link List} of input column names.
     */
    public List<String> getInputNames() {
        return getColumnNames(getInputsIndices());
    }

    /**
     * Get a {@link List} of column names marked as outputs.
     *
     * @return A {@link List} of output column names.
     */
    public List<String> getOutputNames() {
        return getColumnNames(getOutputsIndices());
    }

    /**
     * Sets a column's {@link FeatureDomain}.
     * Setting it to non-empy domain will also mark the column as {@link #isConstrained(int) non-constrained}.
     *
     * @param column Column to add the domain to.
     * @param domain A {@link FeatureDomain} (can be {@link EmptyFeatureDomain}).
     */
    public void setColumnDomain(int column, FeatureDomain domain) {
        validateColumnIndex(column);

        metadata.domains.set(column, domain);
        if (!domain.isEmpty()) {
            metadata.constrained.set(column, false);
        }
    }

    private void validateColumnIndex(int i) {
        final int size = getColumnDimension();
        if (i < 0 || i >= size) {
            throw new IllegalArgumentException("Column " + i + " outside dataframe bounds (0, " + size + ").");
        }
    }

    private void validateRowIndex(int row) {
        final int size = getRowDimension();
        if (row < 0 || row >= size) {
            throw new IllegalArgumentException("Row " + row + " outside dataframe bounds (0, " + size + ").");
        }
    }

    /**
     * Returns whether a column is constrained or not.
     *
     * @param column The column index
     * @return Boolean for constraints.
     */
    public boolean isConstrained(int column) {
        validateColumnIndex(column);
        return metadata.constrained.get(column);
    }

    /**
     * Get the column's {@link Type}.
     *
     * @param column Column index
     * @return The column's {@link Type}.
     */
    public Type getType(int column) {
        validateColumnIndex(column);
        return metadata.types.get(column);
    }

    /**
     * Set the column's type.
     *
     * @param column The column index
     * @param type The column's {@link Type}
     */
    public void setType(int column, Type type) {
        validateColumnIndex(column);
        metadata.types.set(column, type);
    }

    /**
     * Set whether this column is an input or not
     *
     * @param column The column index
     * @param isInput If the column is an input or not
     */
    public void setInput(int column, boolean isInput) {
        validateColumnIndex(column);
        metadata.inputs.set(column, isInput);
    }

    /**
     * Get a column's {@link FeatureDomain}.
     *
     * @param column The column's index.
     * @return The column's {@link FeatureDomain}.
     */
    public FeatureDomain getDomain(int column) {
        validateColumnIndex(column);
        return metadata.domains.get(column);
    }

    /**
     * Calculate a {@link FeatureDomain} based on a column's data.
     *
     * @param column The column to calculate the domain for.
     * @return A {@link FeatureDomain}.
     */
    public FeatureDomain calculateColumnDomain(int column) {
        validateColumnIndex(column);

        final Type type = metadata.types.get(column);

        if (type.equals(Type.NUMBER)) {
            final double max = getColumn(column).stream().mapToDouble(Value::asNumber).max().getAsDouble();
            final double min = getColumn(column).stream().mapToDouble(Value::asNumber).min().getAsDouble();
            return NumericalFeatureDomain.create(min, max);
        } else if (type.equals(Type.BOOLEAN)) {
            final Set<Boolean> categories = getColumn(column).stream().map(value -> (Boolean) value.getUnderlyingObject()).collect(Collectors.toSet());
            return ObjectFeatureDomain.create(categories);
        } else {
            return EmptyFeatureDomain.create();
        }
    }

    /**
     * Returns indices of constrained columns.
     *
     * @return A {@link List} of indices.
     */
    public List<Integer> getConstrained() {
        return columnIndexStream()
                .filter(metadata.constrained::get)
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns the number of rows in this dataframe.
     *
     * @return Number of rows.
     */
    public int getRowDimension() {
        if (data.isEmpty()) {
            return 0;
        } else {
            return this.data.get(0).size();
        }

    }

    /**
     * Sets the number of rows in this dataframe.
     * If the number is smaller than the current number of rows, the rows are truncated.
     * If the number is higher, empty rows are add up to the desired size.
     *
     * @param i New number of rows.
     */
    public void setRowDimension(int i) {
        final int rows = getRowDimension();
        if (i > rows) {
            columnIndexStream().forEach(c -> {
                List<Value> pad = IntStream.range(0, i - rows)
                        .mapToObj(n -> new Value(null))
                        .collect(Collectors.toCollection(ArrayList::new));
                List<Value> values = data.get(c);
                values.addAll(pad);
                data.set(c, values);
            });
        } else if (i < rows) {
            columnIndexStream().forEach(c -> {
                final List<Value> values = data.get(c);
                data.set(c, values.subList(0, i));
            });
        }
    }

    /**
     * Get the number of columns in the {@link Dataframe}.
     *
     * @return Number of columns.
     */
    public int getColumnDimension() {
        return this.data.size();
    }

    public int getInputsCount() {
        return getInputsIndices().size();
    }

    /**
     * Return a column as a {@link List} of {@link Value}.
     *
     * @param column The column to return.
     * @return A {@link List} of {@link Value}.
     */
    public List<Value> getColumn(int column) {
        validateColumnIndex(column);
        return data.get(column);
    }

    /**
     * Return a row as a {@link List} of {@link Value}.
     *
     * @param row The row to return.
     * @return A {@link List} of {@link Value}.
     */
    public List<Value> getRow(int row) {
        validateRowIndex(row);
        return columnIndexStream()
                .mapToObj(column -> data.get(column).get(row))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns all rows as a {@link List} of {@link List<Value>}.
     *
     * @return A {@link List} of {@link List<Value>} rows.
     */
    public List<List<Value>> getRows() {

        return rowIndexStream()
                .mapToObj(row -> columnIndexStream()
                        .mapToObj(column -> safeGetValue(row, column))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<Integer> getInputsIndices() {
        return columnIndexStream()
                .filter(metadata.inputs::get)
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return a stream with all column indices
     *
     * @return A {@link IntStream} of column indices
     */
    public IntStream columnIndexStream() {
        return IntStream.range(0, getColumnDimension());
    }

    /**
     * Return a stream with all row indices
     *
     * @return A {@link IntStream} of row indices
     */
    public IntStream rowIndexStream() {
        return IntStream.range(0, getRowDimension());
    }

    public List<Integer> getOutputsIndices() {
        return columnIndexStream()
                .filter(i -> !metadata.inputs.get(i))
                .boxed().collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Set a column's values.
     * If the row is non-existing or the row's dimensions do not match, it will throw an {@link IllegalArgumentException}.
     *
     * @param column The column to set.
     * @param values The {@link List} of {@link Value} to set.
     */
    public void setColumn(int column, List<Value> values) {
        validateColumnIndex(column);

        if (this.getRowDimension() != values.size()) {
            throw new IllegalArgumentException("Invalid data size. Got " + values.size() + " elements for " + this.getRowDimension() + " rows.");
        }
        this.data.set(column, values);
    }

    /**
     * Set the {@link Value} at row and column.
     *
     * @param row Row to set the value.
     * @param column Column to set the value.
     * @param value The {@link Value} to set.
     */
    public void setValue(int row, int column, Value value) {
        validateColumnIndex(column);
        validateRowIndex(row);
        data.get(column).set(row, value);
    }

    /**
     * Get the {@link Value} at row and column.
     *
     * @param row Row to get the value.
     * @param column Column to get the value.
     * @return value The {@link Value}.
     */
    public Value getValue(int row, int column) {
        validateColumnIndex(column);
        validateRowIndex(row);
        return safeGetValue(row, column);
    }

    private Value safeGetValue(int row, int column) {
        return data.get(column).get(row);
    }

    /**
     * Drop a column from the dataframe.
     * As when deleting an element for a {@link List}, the right-most column indices will be changed.
     *
     * @param column Index of the column to delete.
     */
    public void dropColumn(int column) {
        validateColumnIndex(column);
        data.remove(column);
        metadata.remove(column);
    }

    /**
     * Drop several columns from the dataframe
     *
     * @param columns Indices of the columns to drop
     */
    public void dropColumns(int... columns) {
        Arrays.stream(columns).boxed().sorted(Comparator.reverseOrder()).forEach(this::dropColumn);
    }

    /**
     * Drop several columns from the dataframe
     *
     * @param columns Indices of the columns to drop
     */
    public void dropColumns(List<Integer> columns) {
        columns.stream().sorted(Comparator.reverseOrder()).forEach(this::dropColumn);
    }

    /**
     * Return name of all columns
     *
     * @return A {@link List} with the column names
     */
    public List<String> getColumnNames() {
        return metadata.names;
    }

    /**
     * Return name of specified columns
     *
     * @return A {@link List} with the column names
     */
    public List<String> getColumnNames(List<Integer> indices) {
        return indices.stream().map(metadata.names::get).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return the number of outputs.
     *
     * @return The number of outputs.
     */
    public int getOutputsCount() {
        if (data.isEmpty()) {
            return 0;
        } else {
            return (int) columnIndexStream().filter(i -> !metadata.inputs.get(i)).count();
        }
    }

    /**
     * Return the outputs in a specific row
     * 
     * @param row The specified row
     * @return A {@link List} of {@link Output}
     */
    public List<Output> getOutputRow(int row) {
        validateRowIndex(row);

        final List<Value> rowValues = getRow(row);
        final List<Integer> outputIndices = getOutputsIndices();
        return outputIndices.stream()
                .map(i -> new Output(metadata.names.get(i), metadata.types.get(i), rowValues.get(i), 0.0))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return the input features in a specific row
     *
     * @param row The specified row
     * @return A {@link List} of {@link Feature}
     */
    public List<Feature> getInputRowAsFeature(int row) {
        validateRowIndex(row);

        final List<Value> rowValues = getRow(row);
        final List<Integer> inputIndices = getInputsIndices();
        return inputIndices.stream()
                .map(i -> {
                    if (metadata.constrained.get(i)) {
                        return new Feature(metadata.names.get(i), metadata.types.get(i), rowValues.get(i));
                    } else {
                        return new Feature(metadata.names.get(i), metadata.types.get(i), rowValues.get(i), metadata.constrained.get(i), metadata.domains.get(i));
                    }

                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return the outputs in a specific row
     *
     * @param row The specified row
     * @return A {@link List} of {@link Output}
     */
    public List<Output> getOutputRowAsOutput(int row) {
        validateRowIndex(row);

        final List<Value> rowValues = getRow(row);
        final List<Integer> outputIndices = getOutputsIndices();
        return outputIndices.stream()
                .map(i -> {
                    return new Output(metadata.names.get(i), metadata.types.get(i), rowValues.get(i), 0.0);
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return the input values in a specific row
     *
     * @param row The specified row
     * @return A {@link List} of {@link Feature}
     */
    public List<Value> getInputRow(int row) {
        validateRowIndex(row);

        final List<Value> rowValues = getRow(row);
        final List<Integer> inputIndices = getInputsIndices();
        return inputIndices.stream()
                .map(rowValues::get)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return the input values for all rows
     *
     * @return A {@link List} of {@link List<Value>}
     */
    public List<List<Value>> getInputRows() {

        final List<Integer> inputColumns = getInputsIndices();

        return rowIndexStream()
                .mapToObj(row -> inputColumns.stream()
                        .map(column -> safeGetValue(row, column))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .collect(Collectors.toCollection(ArrayList::new));

    }

    /**
     * Return the output values for all rows
     *
     * @return A {@link List} of {@link List<Value>}
     */
    public List<List<Value>> getOutputRows() {

        final List<Integer> outputColumns = getOutputsIndices();

        return rowIndexStream()
                .mapToObj(row -> outputColumns.stream()
                        .map(column -> safeGetValue(row, column))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .collect(Collectors.toCollection(ArrayList::new));

    }

    /**
     * Copy this dataframe as a new one.
     *
     * @return A {@link Dataframe}
     */
    public Dataframe copy() {
        return new Dataframe(
                this.data.stream().map(ArrayList::new).collect(Collectors.toCollection(ArrayList::new)),
                metadata.copy());
    }

    /**
     * Return a column as a {@link List} of {@link Feature}.
     *
     * @param column The column to get.
     * @return A {@link List} of {@link Feature}.
     */
    public List<Feature> columnAsFeatures(int column) {
        validateColumnIndex(column);

        final Type type = metadata.types.get(column);
        if (type.equals(Type.NUMBER)) {
            return data.get(column).stream()
                    .map(v -> FeatureFactory.newNumericalFeature(metadata.names.get(column), (Number) v.getUnderlyingObject(), metadata.domains.get(column)))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else if (type.equals(Type.BOOLEAN)) {
            return data.get(column).stream()
                    .map(v -> FeatureFactory.newBooleanFeature(metadata.names.get(column), (Boolean) v.getUnderlyingObject(), metadata.domains.get(column)))
                    .collect(Collectors.toCollection(ArrayList::new));
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Return a column as a {@link List} of {@link Output}.
     *
     * @param column The column to get.
     * @return A {@link List} of {@link Output}.
     */
    public List<Output> columnAsOutputs(int column) {
        validateColumnIndex(column);

        return data.get(column)
                .stream()
                .map(v -> new Output(metadata.names.get(column), metadata.types.get(column), v, 0.0))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return a {@link Dataframe} with only the selected rows.
     *
     * @param rows Rows to include in new {@link Dataframe}.
     * @return A filtered {@link Dataframe}.
     */
    public Dataframe filterByRowIndex(List<Integer> rows) {

        final Metadata metadataCopy = metadata.copy();

        final List<List<Value>> dataCopy = columnIndexStream().mapToObj(col -> {
            final List<Value> column = data.get(col);
            return rows.stream().map(column::get).collect(Collectors.toCollection(ArrayList::new));
        }).collect(Collectors.toCollection(ArrayList::new));

        return new Dataframe(dataCopy, metadataCopy);
    }

    /**
     * Return a new {@link Dataframe} for which only the rows where the specified column satisfy the {@link Predicate<Value>}.
     *
     * @param column Column to use for filtering
     * @param predicate {@link Predicate<Value>} to select rows
     * @return A new {@link Dataframe}
     */
    public Dataframe filterByColumnValue(int column, Predicate<Value> predicate) {
        validateColumnIndex(column);

        final List<Value> values = data.get(column);

        final List<Integer> rowIndices = rowIndexStream()
                .filter(i -> predicate.test(values.get(i))).boxed()
                .collect(Collectors.toUnmodifiableList());

        return filterByRowIndex(rowIndices);
    }

    public Dataframe filterRowsByInputs(Predicate<List<Value>> predicate) {
        final List<List<Value>> inputRows = getInputRows();
        return filterRowsByColumnRole(inputRows, predicate);
    }

    public Dataframe filterRowsByOutputs(Predicate<List<Value>> predicate) {
        final List<List<Value>> outputRows = getOutputRows();
        return filterRowsByColumnRole(outputRows, predicate);
    }

    private Dataframe filterRowsByColumnRole(final List<List<Value>> roleRows, final Predicate<List<Value>> predicate) {
        final List<Integer> filteredRowIndices = rowIndexStream()
                .filter(rowNumber -> predicate.test(roleRows.get(rowNumber)))
                .boxed()
                .collect(Collectors.toList());

        return filterByRowIndex(filteredRowIndices);

    }

    /**
     * Apply a {@link Function<Value,Value>} to all the values in a column.
     *
     * @param column The column to apply the function.
     * @param fn {@link Function<Value,Value>} to apply.
     */
    public void transformColumn(int column, Function<Value, Value> fn) {
        validateColumnIndex(column);
        final List<Value> transformedColumn = data.get(column).stream()
                .map(fn)
                .collect(Collectors.toCollection(ArrayList::new));
        data.set(column, transformedColumn);
    }

    /**
     * Sort all rows according to a supplied {@link Comparator} applied to a column.
     *
     * @param column The column to sort by
     * @param comparator A supplied {@link Comparator}
     */
    public void sortRowsByColumn(int column, Comparator<Value> comparator) {
        validateColumnIndex(column);
        final List<Value> columnValues = data.get(column);
        // Calculate sort indices
        final int[] sortedIndices = rowIndexStream().boxed().sorted((i, j) -> comparator.compare(columnValues.get(i), columnValues.get(j))).mapToInt(n -> n).toArray();
        // Apply new indices to all columns
        columnIndexStream().forEach(c -> {
            final List<Value> columnValuesUnsorted = data.get(c);
            final List<Value> columnValuesSorted = Arrays.stream(sortedIndices)
                    .mapToObj(columnValuesUnsorted::get)
                    .collect(Collectors.toCollection(ArrayList::new));
            data.set(c, columnValuesSorted);
        });
    }

    /**
     * Apply a {@link Function<Value,Value>} to all the values in a row.
     *
     * @param row Row to apply the function.
     * @param fn {@link Function<Value,Value>} to apply.
     */
    public void transformRow(int row, Function<Value, Value> fn) {
        validateRowIndex(row);
        final List<Value> transformedRow = columnIndexStream()
                .mapToObj(column -> data.get(column).get(row))
                .map(fn)
                .collect(Collectors.toUnmodifiableList());
        columnIndexStream().forEach(column -> data.get(column).set(row, transformedRow.get(column)));
    }

    /**
     * Combine all the {@link Value} in a row into a single one and return it.
     *
     * @param row Row to apply the reduce function.
     * @param fn Reduce {@link Function<List<Value>,Value>} to apply.
     * @return Resulting {@link Value}.
     */
    public Value reduceRow(int row, Function<List<Value>, Value> fn) {
        validateRowIndex(row);
        return fn.apply(getRow(row));
    }

    /**
     * Combine all the {@link Value} in all rows into a {@link List} and return it.
     * Since the result list's dimension is the same as the number of rows, this is equivalent to reducing all columns into a single one.
     *
     * @param fn Reduce {@link Function<List<Value>,Value>} to apply.
     * @return Resulting {@link List} {@link Value}.
     */
    public List<Value> reduceRows(Function<List<Value>, Value> fn) {
        return rowIndexStream()
                .mapToObj(row -> fn.apply(getRow(row)))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public void addColumn(String name, Type type, List<Value> values) {
        data.add(new ArrayList<>(values));

        metadata.names.add(name);
        metadata.types.add(type);
        metadata.constrained.add(true);
        metadata.domains.add(EmptyFeatureDomain.create());
        metadata.inputs.add(false);
    }

    public List<Prediction> asPredictions() {
        final List<Integer> inputIndices = getInputsIndices();
        final List<Integer> outputIndices = getOutputsIndices();
        final List<List<Feature>> allInputs = getInputsIndices().stream().map(this::columnAsFeatures).collect(Collectors.toUnmodifiableList());
        final List<List<Output>> allOutputs = getOutputsIndices().stream().map(this::columnAsOutputs).collect(Collectors.toUnmodifiableList());

        final List<Prediction> predictions = new ArrayList<>();
        for (int row = 0; row < this.getRowDimension(); row++) {
            List<Feature> features = new ArrayList<>();
            for (int col = 0; col < inputIndices.size(); col++) {
                features.add(allInputs.get(col).get(row));
            }
            List<Output> outputs = new ArrayList<>();
            for (int col = 0; col < outputIndices.size(); col++) {
                outputs.add(allOutputs.get(col).get(row));
            }
            final PredictionInput input = new PredictionInput(features);
            final PredictionOutput output = new PredictionOutput(outputs);
            predictions.add(new SimplePrediction(input, output));
        }
        return predictions;
    }

    public List<PredictionInput> asPredictionInputs() {
        final List<Integer> inputIndices = getInputsIndices();
        final List<List<Feature>> allInputs = getInputsIndices().stream().map(this::columnAsFeatures).collect(Collectors.toUnmodifiableList());

        final List<PredictionInput> predictions = new ArrayList<>();
        for (int row = 0; row < this.getRowDimension(); row++) {
            List<Feature> features = new ArrayList<>();
            for (int col = 0; col < inputIndices.size(); col++) {
                features.add(allInputs.get(col).get(row));
            }
            final PredictionInput input = new PredictionInput(features);
            predictions.add(input);
        }
        return predictions;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Dataframe [").append(getRowDimension()).append("x").append(getColumnDimension()).append("]\n");
        for (int i = 0; i < getColumnDimension(); i++) {
            builder.append("\tColumn ").append(i).append(" (").append(metadata.names.get(i)).append(")\n");
            builder.append("\t\tType: ").append(metadata.types.get(i)).append("\n");
            builder.append("\t\tDomain: ");
            final FeatureDomain domain = metadata.domains.get(i);
            if (domain.isEmpty()) {
                builder.append("(no domain)");
            } else {
                builder.append(domain.prettyPrint());
            }
            builder.append("\n");
            builder.append("\t\tInput: ").append(metadata.inputs.get(i) ? "yes" : "no").append("\n");
        }
        return builder.toString();
    }

    private class Metadata {
        private final List<String> names;
        private final List<Type> types;
        private final List<Boolean> constrained;
        private final List<FeatureDomain> domains;
        private final List<Boolean> inputs;

        private Metadata() {
            this(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        private Metadata(List<String> names, List<Type> types, List<Boolean> constrained, List<FeatureDomain> domains, List<Boolean> inputs) {
            this.names = new ArrayList<>(names);
            this.types = new ArrayList<>(types);
            this.constrained = new ArrayList<>(constrained);
            this.domains = new ArrayList<>(domains);
            this.inputs = new ArrayList<>(inputs);
        }

        public void remove(int column) {
            names.remove(column);
            types.remove(column);
            constrained.remove(column);
            domains.remove(column);
            inputs.remove(column);
        }

        public Metadata copy() {
            return new Metadata(
                    new ArrayList<>(this.names),
                    new ArrayList<>(this.types),
                    new ArrayList<>(this.constrained),
                    new ArrayList<>(this.domains),
                    new ArrayList<>(this.inputs));
        }

    }
}
