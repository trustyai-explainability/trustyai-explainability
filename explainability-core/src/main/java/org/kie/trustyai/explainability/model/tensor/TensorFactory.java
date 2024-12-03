package org.kie.trustyai.explainability.model.tensor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.stream.Stream;

public class TensorFactory {
    // == DIMENSIONALITY HELPERS =======================================================================================
    private static <T> int[] get2DDimensions(T[][] array) {
        return new int[] { array.length, array[0].length };
    }

    private static <T> int[] get3DDimensions(T[][][] array) {
        return new int[] { array.length, array[0].length, array[0][0].length };
    }

    private static <T> int[] get4DDimensions(T[][][][] array) {
        return new int[] { array.length, array[0].length, array[0][0].length, array[0][0][0].length };
    }

    // == FLATTENER ====================================================================================================
    private static Stream<Object> flatten(Object[] array) {
        return Arrays.stream(array)
                .flatMap(o -> o instanceof Object[] ? flatten((Object[]) o) : Stream.of(o));
    }

    // == TENSOR BUILDERS ==============================================================================================
    public static <T> Tensor<T> fromArray(T[] array) {
        return new Tensor<>(array, new int[] { array.length });
    }

    public static <T> Tensor<T> fromArray(T[][] array) {
        int[] dimensions = get2DDimensions(array);
        T[] flatArray = (T[]) flatten(array).toArray();
        return new Tensor<>(flatArray, dimensions);
    }

    public static <T> Tensor<T> fromArray(T[][][] array) {
        int[] dimensions = get3DDimensions(array);
        T[] flatArray = (T[]) flatten(array).toArray();
        return new Tensor<>(flatArray, dimensions);
    }

    public static <T> Tensor<T> fromArray(T[][][][] array) {
        int[] dimensions = get4DDimensions(array);
        T[] flatArray = (T[]) flatten(array).toArray();
        return new Tensor<>(flatArray, dimensions);
    }

    // == TENSOR CONVERTERS ============================================================================================
    public static <T> T[] to1DArray(Tensor<T> tensor) {
        if (tensor.getDimension() != 1) {
            throw new IllegalArgumentException("The 1D array conversion can only be used with 1D tensors");
        }
        return tensor.getData();
    }

    public static <T> T[][] to2DArray(Tensor<T> tensor) {
        if (tensor.getDimension() != 2) {
            throw new IllegalArgumentException("The 2D array conversion can only be used with 2D tensors");
        }
        T[][] arr = (T[][]) Array.newInstance(tensor.getDatatype(), tensor.getDimensions());
        for (int i = 0; i < tensor.getnEntries(); i++) {
            int[] coords = tensor.getCoordinates(i);
            arr[coords[0]][coords[1]] = tensor.getData()[i];
        }
        return arr;
    }

    public static <T> T[][][] to3DArray(Tensor<T> tensor) {
        if (tensor.getDimension() != 3) {
            throw new IllegalArgumentException("The 3D array conversion can only be used with 3D tensors");
        }

        T[][][] arr = (T[][][]) Array.newInstance(tensor.getDatatype(), tensor.getDimensions());
        for (int i = 0; i < tensor.getnEntries(); i++) {
            int[] coords = tensor.getCoordinates(i);
            arr[coords[0]][coords[1]][coords[2]] = tensor.getData()[i];
        }
        return arr;

    }

    public static <T> T[][][][] to4DArray(Tensor<T> tensor) {
        if (tensor.getDimension() != 4) {
            throw new IllegalArgumentException("The 4D array conversion can only be used with 4D tensors");
        }

        T[][][][] arr = (T[][][][]) Array.newInstance(tensor.getDatatype(), tensor.getDimensions());
        for (int i = 0; i < tensor.getnEntries(); i++) {
            int[] coords = tensor.getCoordinates(i);
            arr[coords[0]][coords[1]][coords[2]][coords[3]] = tensor.getData()[i];
        }
        return arr;

    }

}
