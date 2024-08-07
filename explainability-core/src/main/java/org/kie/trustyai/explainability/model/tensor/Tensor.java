package org.kie.trustyai.explainability.model.tensor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Tensor<T> {
    private int[] dimensions;
    private T[] data;
    private int dimension;
    private int nEntries;
    private Class datatype;

    Tensor(T[] data, int[] dimensions) {
        this.data = data;
        this.dimensions = dimensions;
        this.dimension = dimensions.length;
        this.nEntries = vectorProduct(dimensions);
        this.datatype = data[0].getClass();
    }

    // == LINEAR INDEX CONVERSIONS =====================================================================================
    // convert a set of N-dimensional tensor coordinates to the corresponding linear index
    protected int getLinearIndex(int... indices) {
        if (indices.length != dimensions.length) {
            throw new IllegalArgumentException(String.format(
                    "To retrieve a single element of a tensor, the passed list of indices must have equal length to the" +
                            " tensor dimension. Received %d indices, but the tensor dimension is %d.",
                    indices.length, dimension));
        }

        // map indices to linear index
        int linearIndex = 0;
        int product = 1;

        for (int i = dimension - 1; i >= 0; i--) {
            linearIndex += indices[i] * product;
            product *= dimensions[i];
        }
        return linearIndex;
    }

    // convert a linear index into the corresponding N-dimensional tensor coordinates
    protected int[] getCoordinates(int linearIndex) {
        int[] coords = new int[dimension];
        for (int i = dimension - 1; i >= 0; i--) {
            coords[i] = linearIndex % dimensions[i];
            linearIndex = Math.floorDiv(linearIndex, dimensions[i]);
        }
        return coords;
    }

    // == TENSOR SLICING AND ELEMENT ACCESS ============================================================================
    /**
     * Retrieve a single value from the n-dimensional tensor
     *
     * @param indices : the coordinates of the value to retrieve
     *
     * @returns the value at the provided coordinates
     **/
    public T getElement(int... indices) {
        return this.data[getLinearIndex(indices)];
    }

    /**
     * Slice the n-dimensional tensor, emulating the Numpy slicing syntax
     *
     * @param slices : the per-axis slices to perform
     *
     * @returns the sliced tensor
     **/
    public Tensor<T> slice(Slice... slices) {
        if (slices.length > dimensions.length) {
            throw new IllegalArgumentException(String.format(
                    "%d slice dimensions were passed, but this tensor only has %d dimensions",
                    slices.length, dimension));
        }

        // match slice size to tensor dimension
        List<Slice> effectiveSlices = new ArrayList<>(List.of(slices));
        while (effectiveSlices.size() < dimension) {
            effectiveSlices.add(Slice.all());
        }

        // get new dimensionality and coordinate bounds of each slice
        List<Integer> slicedDimensions = new ArrayList<>();
        int[][] sliceBounds = new int[dimension][2];
        for (int i = 0; i < dimension; i++) {
            Slice s = effectiveSlices.get(i);
            switch (s.getSliceType()) {
                case ALL:
                    slicedDimensions.add(dimensions[i]);
                    sliceBounds[i] = new int[] { 0, dimensions[i] };
                    break;
                case AT:
                    sliceBounds[i] = new int[] { s.at(), s.at() + 1 };
                    break;
                case BETWEEN:
                    sliceBounds[i] = new int[] { s.from(), s.to() };
                    slicedDimensions.add(s.to() - s.from());
                    break;
                case TO:
                    sliceBounds[i] = new int[] { 0, s.to() };
                    slicedDimensions.add(s.to());
                    break;
                case FROM:
                    sliceBounds[i] = new int[] { s.from(), dimensions[i] };
                    slicedDimensions.add(dimensions[i] - s.from());
                    break;
            }
        }

        // collect values that are contained within the slice
        // this is O(n*d) where n=nEntries, d=dimensions, so might not be the most efficient implementation?
        int slicedEntries = slicedDimensions.stream().reduce(1, (a, b) -> a * b);
        T[] slicedData = (T[]) Array.newInstance(datatype, slicedEntries);
        int sliceIdx = 0;
        for (int i = 0; i < nEntries; i++) {
            int[] dimensionalIndex = getCoordinates(i);
            boolean inSlice = true;
            for (int d = 0; d < dimension; d++) {
                if (!(sliceBounds[d][0] <= dimensionalIndex[d] && dimensionalIndex[d] < sliceBounds[d][1])) {
                    inSlice = false;
                    break;
                }
            }
            if (inSlice) {
                slicedData[sliceIdx] = data[i];
                sliceIdx++;
            }
        }

        int[] slicedDimensionsArr = slicedDimensions.stream().mapToInt(i -> i).toArray();
        return new Tensor<>(slicedData, slicedDimensionsArr);
    }

    // == UTILITY ======================================================================================================
    // get the product of all elements within a vector
    private static int vectorProduct(int[] vector) {
        return vectorProduct(vector, 0);
    }

    // get the product of all elements in the subvector from index "from" to the end
    private static int vectorProduct(int[] vector, int from) {
        int[] slicedV;
        if (from != 0) {
            slicedV = Arrays.copyOfRange(vector, from, vector.length);
        } else {
            slicedV = vector;
        }
        return Arrays.stream(slicedV).reduce(1, (a, b) -> a * b);
    }

    // flatten a nested list of arbitrary depth
    private static <T> List<T> flattenList(List<?> list) {
        List<T> values = new ArrayList<>();
        if (list.get(0) instanceof List) {
            for (List sublist : (List<List>) list) {
                values.addAll(flattenList(sublist));
            }
        } else {
            values.addAll((List<T>) list);
        }
        return values;
    }

    private static List<Integer> getNestedListDimensions(List<?> list, List<Integer> dimensions) {
        dimensions.add(list.size());
        if (list.get(0) instanceof List) {
            getNestedListDimensions((List) list.get(0), dimensions);
        }
        return dimensions;
    }

    // given a flat list and a set of dimensions, nest the list into the provided shape
    private static <T> List nestList(List<T> flattenedList, int[] dimensions) {
        int prod = vectorProduct(dimensions);
        if (prod != flattenedList.size()) {
            throw new IllegalArgumentException("Shapes not compatible in nestList, received list of size " +
                    flattenedList.size() + " but the product of the provided dimensions is " + prod);
        }

        if (dimensions.length == 1) {
            return flattenedList;
        } else {
            List output = new ArrayList();
            int stride = vectorProduct(dimensions, 1);
            for (int i = 0; i < dimensions[0]; i++) {
                int startIdx = stride * i;
                int endIdx = stride * (i + 1);
                output.add(nestList(flattenedList.subList(startIdx, endIdx), Arrays.copyOfRange(dimensions, 1, dimensions.length)));
            }
            return output;
        }
    }

    // == LIST CONVERSIONS =============================================================================================
    /**
     * Create a tensor from a nested list
     *
     * @param list a set nested lists, where the bottom-most list is of type T
     *
     * @returns the resultant tensor of the same dimensions as the nested list
     **/
    public static <T> Tensor<T> fromList(List<?> list) {
        List<T> dataList = flattenList(list);
        int[] dimensions = getNestedListDimensions(list, new ArrayList<>()).stream().mapToInt(i -> i).toArray();
        T[] dataArr = (T[]) Array.newInstance(dataList.get(0).getClass(), dataList.size());
        dataArr = dataList.toArray(dataArr);
        return new Tensor<>(dataArr, dimensions);
    }

    /**
     * Convert a tensor to a nested list
     *
     * @returns a set of nested lists of the same dimension as the tensor.
     **/
    public List toNestedList() {
        return nestList(Arrays.stream(this.data).collect(Collectors.toList()), dimensions);
    }

    // == I/O ==========================================================================================================
    // convert the tensor to string, emulating Numpy's tensor printing
    public String toString() {
        String recursiveString = recursiveString(Arrays.stream(this.data).collect(Collectors.toList()), dimensions, "       ");
        return String.format("tensor(%s, dtype=%s)", recursiveString, datatype.getName());
    }

    // emulate Numpy's tensor printing
    public static <T> String recursiveString(List<T> flattenedList, int[] dimensions, String indent) {
        if (dimensions.length == 1) {
            return flattenedList.toString();
        } else {
            StringBuilder output = new StringBuilder();
            int stride = vectorProduct(dimensions, 1);
            for (int i = 0; i < dimensions[0]; i++) {
                int startIdx = stride * i;
                int endIdx = stride * (i + 1);

                int[] nextDimension = Arrays.copyOfRange(dimensions, 1, dimensions.length);
                String result = recursiveString(flattenedList.subList(startIdx, endIdx), nextDimension, indent + " ");
                if (nextDimension.length == 1) {
                    result = result.replace("[", "").replace("]", "");
                }

                output.append((i == 0 ? "" : indent) + "[");
                output.append(result);
                output.append("]");

                if (i != dimensions[0] - 1) {
                    output.append(",");
                    output.append(System.getProperty("line.separator"));
                    if (nextDimension.length > 1) {
                        output.append(System.getProperty("line.separator"));
                    }
                    if (nextDimension.length > 2) {
                        output.append(System.getProperty("line.separator"));
                    }
                }

            }
            return output.toString();
        }
    }

    // == GETTERS ======================================================================================================
    public int[] getDimensions() {
        return dimensions;
    }

    public int getDimensions(int axis) {
        return dimensions[axis];
    }

    public T[] getData() {
        return data;
    }

    public Class getDatatype() {
        return datatype;
    }

    public int getDimension() {
        return dimension;
    }

    public int getnEntries() {
        return nEntries;

    }

    // == EQUALS =======================================================================================================
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Tensor))
            return false;
        Tensor<?> tensor = (Tensor<?>) o;
        return Objects.deepEquals(dimensions, tensor.dimensions) && Objects.deepEquals(data, tensor.data) && Objects.equals(datatype, tensor.datatype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(dimensions), Arrays.hashCode(data), dimension, nEntries, datatype);
    }
}
