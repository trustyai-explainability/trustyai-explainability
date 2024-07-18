package org.kie.trustyai.explainability.model.tensor;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TensorFactoryTest {

    Random rng = new Random(0L);

    // these functions build an array where the value of the element at position[coords] equals the linear index of those coords
    Integer[][] get2DArr(int[] dimension) {
        int a = dimension[0];
        int b = dimension[1];
        Integer[][] arr = new Integer[a][b];
        int idx = 0;
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                arr[i][ii] = idx;
                idx += 1;
            }
        }
        return arr;
    }

    Integer[][][] get3DArr(int[] dimension) {
        int a = dimension[0];
        int b = dimension[1];
        int c = dimension[2];
        Integer[][][] arr = new Integer[a][b][c];
        int idx = 0;
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                for (int iii = 0; iii < c; iii++) {
                    arr[i][ii][iii] = idx;
                    idx += 1;
                }
            }
        }
        return arr;
    }

    Integer[][][][] get4DArr(int[] dimension) {
        int a = dimension[0];
        int b = dimension[1];
        int c = dimension[2];
        int d = dimension[3];
        Integer[][][][] arr = new Integer[a][b][c][d];
        int idx = 0;
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                for (int iii = 0; iii < c; iii++) {
                    for (int iv = 0; iv < d; iv++) {
                        arr[i][ii][iii][iv] = idx;
                        idx += 1;
                    }
                }
            }
        }
        return arr;
    }

    int[] getRandomCoords(int[] dimension) {
        int[] coords = new int[dimension.length];
        for (int i = 0; i < dimension.length; i++) {
            coords[i] = rng.nextInt(dimension[i]);
        }
        return coords;
    }

    // == ARRAY -> TENSOR ==============================================================================================
    @Test
    @DisplayName("Convert 2D Array to Tensor")
    void testConvert2DArrayToTensor() {
        int[] dimension = { 10, 11 };
        Integer[][] arr = get2DArr(dimension);
        Tensor<Integer> tensor = TensorFactory.fromArray(arr);

        assertArrayEquals(dimension, tensor.getDimensions());

        for (int i = 0; i < 100; i++) {
            int[] coords = getRandomCoords(dimension);
            int linearIdx = tensor.getLinearIndex(coords);

            // the element at this coordinates should equal the linearIdx of that coordinate
            assertEquals(linearIdx, tensor.getElement(coords));
        }
    }

    @Test
    @DisplayName("Convert 3D Array to Tensor")
    void testConvert3DArrayToTensor() {
        int[] dimension = { 10, 11, 12 };
        Integer[][][] arr = get3DArr(dimension);
        Tensor<Integer> tensor = TensorFactory.fromArray(arr);

        assertArrayEquals(dimension, tensor.getDimensions());

        for (int i = 0; i < 100; i++) {
            int[] coords = getRandomCoords(dimension);
            int linearIdx = tensor.getLinearIndex(coords);

            // the element at this coordinates should equal the linearIdx of that coordinate
            assertEquals(linearIdx, tensor.getElement(coords));
        }
    }

    @Test
    @DisplayName("Convert 4D Array to Tensor")
    void testConvert4DArrayToTensor() {
        int[] dimension = { 10, 11, 12, 13 };
        Integer[][][][] arr = get4DArr(dimension);
        Tensor<Integer> tensor = TensorFactory.fromArray(arr);

        assertArrayEquals(dimension, tensor.getDimensions());

        for (int i = 0; i < 100; i++) {
            int[] coords = getRandomCoords(dimension);
            int linearIdx = tensor.getLinearIndex(coords);

            // the element at this coordinates should equal the linearIdx of that coordinate
            assertEquals(linearIdx, tensor.getElement(coords));
        }
    }

    // == TENSOR -> ARRAY ==============================================================================================
    @Test
    @DisplayName("Convert 2D Tensor to Array")
    void testConvert2DTensorToArray() {
        int[] dimension = { 10, 11 };
        Integer[][] arr = get2DArr(dimension);
        Tensor<Integer> tensor = TensorFactory.fromArray(arr);
        assertArrayEquals(arr, TensorFactory.to2DArray(tensor));
        assertThrows(IllegalArgumentException.class, () -> TensorFactory.to3DArray(tensor));
    }

    @Test
    @DisplayName("Convert 3D Tensor to Array")
    void testConvert3DTensorToArray() {
        int[] dimension = { 10, 11, 12 };
        Integer[][][] arr = get3DArr(dimension);
        Tensor<Integer> tensor = TensorFactory.fromArray(arr);
        assertArrayEquals(arr, TensorFactory.to3DArray(tensor));
        assertThrows(IllegalArgumentException.class, () -> TensorFactory.to4DArray(tensor));
    }

    @Test
    @DisplayName("Convert 4D Tensor to Array")
    void testConvert4DTensorToArray() {
        int[] dimension = { 10, 11, 12, 13 };
        Integer[][][][] arr = get4DArr(dimension);
        Tensor<Integer> tensor = TensorFactory.fromArray(arr);
        assertArrayEquals(arr, TensorFactory.to4DArray(tensor));
        assertThrows(IllegalArgumentException.class, () -> TensorFactory.to2DArray(tensor));
    }
}
