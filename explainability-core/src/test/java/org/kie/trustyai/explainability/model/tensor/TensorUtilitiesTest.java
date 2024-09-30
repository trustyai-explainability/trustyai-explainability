package org.kie.trustyai.explainability.model.tensor;

import java.util.Random;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.PerturbationContext;

import static org.junit.jupiter.api.Assertions.*;

class TensorUtilitiesTest {
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

    String[][][] get3DStringArr(int[] dimension) {
        int a = dimension[0];
        int b = dimension[1];
        int c = dimension[2];
        String[][][] arr = new String[a][b][c];
        int idx = 0;
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                for (int iii = 0; iii < c; iii++) {
                    arr[i][ii][iii] = String.valueOf(idx);
                    idx += 1;
                }
            }
        }
        return arr;
    }

    PerturbationContext pc = new PerturbationContext(new Random(0L), 0);

    @Test
    void testRandomIntFill() {
        int[] shape = new int[] { 5, 4, 3 };
        Tensor<Integer> tensor = TensorFactory.fromArray(get3DArr(shape));
        tensor = TensorUtilities.randomIntFill(tensor, pc);
        assertEquals(Integer.class, tensor.getData()[0].getClass());
        assertArrayEquals(shape, tensor.getDimensions());
    }

    @Test
    void testRandomLongFill() {
        int[] shape = new int[] { 5, 4, 3 };
        Tensor<Integer> tensor = TensorFactory.fromArray(get3DArr(shape));
        Tensor<Long> tensor2 = TensorUtilities.randomLongFill(tensor, pc);
        assertEquals(Long.class, tensor2.getData()[0].getClass());
        assertArrayEquals(shape, tensor2.getDimensions());
    }

    @Test
    void testRandomFloatFill() {
        int[] shape = new int[] { 5, 4, 3 };
        Tensor<Integer> tensor = TensorFactory.fromArray(get3DArr(shape));
        Tensor<Float> tensor2 = TensorUtilities.randomFloatFill(tensor, pc);
        assertEquals(Float.class, tensor2.getData()[0].getClass());
        assertArrayEquals(shape, tensor2.getDimensions());
    }

    @Test
    void testRandomDoubleFill() {
        int[] shape = new int[] { 5, 4, 3 };
        Tensor<Integer> tensor = TensorFactory.fromArray(get3DArr(shape));
        Tensor<Double> tensor2 = TensorUtilities.randomDoubleFill(tensor, pc);
        assertEquals(Double.class, tensor2.getData()[0].getClass());
        assertArrayEquals(shape, tensor2.getDimensions());
    }

    @Test
    void testRandomBooleanFill() {
        int[] shape = new int[] { 5, 4, 3 };
        Tensor<Integer> tensor = TensorFactory.fromArray(get3DArr(shape));
        Tensor<Boolean> tensor2 = TensorUtilities.randomBooleanFill(tensor, pc);
        assertEquals(Boolean.class, tensor2.getData()[0].getClass());
        assertArrayEquals(shape, tensor2.getDimensions());
    }

    @Test
    void testRandomByteFill() {
        int[] shape = new int[] { 5, 4, 3 };
        Tensor<Integer> tensor = TensorFactory.fromArray(get3DArr(shape));
        Tensor<Byte> tensor2 = TensorUtilities.randomByteFill(tensor, pc);
        assertEquals(Byte.class, tensor2.getData()[0].getClass());
        assertArrayEquals(shape, tensor2.getDimensions());
    }

    @Test
    void testRandomStringFillLengthMatching() {
        int[] shape = new int[] { 11, 5, 2 };
        Tensor<String> tensor = TensorFactory.fromArray(get3DStringArr(shape));
        tensor = TensorUtilities.randomStringFill(tensor, pc);
        assertEquals(String.class, tensor.getData()[0].getClass());
        assertEquals(1, tensor.getData()[1].length());
        assertEquals(2, tensor.getData()[10].length());
        assertEquals(3, tensor.getData()[100].length());
        assertArrayEquals(shape, tensor.getDimensions());
    }

    @Test
    void testRandomStringFillConstantLength() {
        int[] shape = new int[] { 11, 5, 2 };
        Tensor<String> tensor = TensorFactory.fromArray(get3DStringArr(shape));
        tensor = TensorUtilities.randomStringFill(tensor, 16, pc);
        assertEquals(String.class, tensor.getData()[0].getClass());
        assertEquals(16, tensor.getData()[1].length());
        assertEquals(16, tensor.getData()[10].length());
        assertEquals(16, tensor.getData()[100].length());
        assertArrayEquals(shape, tensor.getDimensions());
    }

}
