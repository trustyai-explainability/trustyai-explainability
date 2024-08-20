package org.kie.trustyai.explainability.model.tensor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.metrics.utils.ArrayGenerators;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TensorTest {
    Random rng = new Random(0L);

    public List<?> generate(int[] shape) {
        return generate(shape, null);
    }

    public String label(int idx, String prefix) {
        if (null == prefix) {
            return String.valueOf(idx);
        } else {
            return prefix + "," + idx;
        }
    }

    public List<?> generate(int[] shape, String prefix) {
        if (shape.length == 1) {
            return IntStream.range(0, shape[0]).mapToObj(idx -> label(idx, prefix)).collect(Collectors.toList());
        } else {
            // recursively generate
            return IntStream.range(0, shape[0]).mapToObj(idx -> generate(Arrays.copyOfRange(shape, 1, shape.length), label(idx, prefix))).collect(Collectors.toList());
        }
    }

    @Test
    @DisplayName("Conversion from coordinates to linear index and vice versa is consistent")
    void testIndex() {
        int[] dimensions = { 10, 11, 12, 13, 14 };
        List<?> data = generate(dimensions);
        Tensor<String> tensor = Tensor.fromList(data);
        for (int i = 0; i < 100; i++) {
            int a = rng.nextInt(10);
            int b = rng.nextInt(11);
            int c = rng.nextInt(12);
            int d = rng.nextInt(13);
            int e = rng.nextInt(14);
            int[] coords = { a, b, c, d, e };
            assertArrayEquals(coords, tensor.getCoordinates(tensor.getLinearIndex(coords)));
        }
    }

    @Test
    @DisplayName("Indexing a tensor by dimensioned coordinates returns the expected value")
    void testGetElement() {
        int[] dimensions = { 10, 10, 10, 10, 10 };
        List<?> data = generate(dimensions);
        Tensor<String> tensor = Tensor.fromList(data);
        assertEquals(100_000, tensor.getnEntries());

        for (int i = 0; i < 100; i++) {
            int a = rng.nextInt(10);
            int b = rng.nextInt(10);
            int c = rng.nextInt(10);
            int d = rng.nextInt(10);
            int e = rng.nextInt(10);

            assertEquals(String.format("%d,%d,%d,%d,%d", a, b, c, d, e), tensor.getElement(a, b, c, d, e));
        }
    }

    @Test
    @DisplayName("Slicing a tensor is consistent with Numpy")
    void testSlice() {
        // tensor 1
        int[] dimensions = { 11, 12, 13, 14, 15 };
        List<?> data = generate(dimensions);
        Tensor<String> tensor = Tensor.fromList(data);
        Tensor<String> sliced = tensor.slice(
                Slice.at(5),
                Slice.between(0, 5),
                Slice.all(),
                Slice.to(2),
                Slice.from(7));
        assertArrayEquals(new int[] { 5, 13, 2, 8 }, sliced.getDimensions());
        assertEquals(1040, sliced.getnEntries());

        //check various slicex
        assertEquals("5,3,4,1,10", sliced.getElement(3, 4, 1, 3));
        assertEquals("5,1,1,1,8", sliced.getElement(1, 1, 1, 1));
        assertEquals("5,1,1,1,12", sliced.getElement(1, 1, 1, 5));
        assertEquals("5,3,11,1,13", sliced.getElement(3, 11, 1, 6));
        assertEquals("5,1,2,0,11", sliced.getElement(1, 2, 0, 4));
        assertEquals("5,4,11,1,7", sliced.getElement(4, 11, 1, 0));
        assertEquals("5,1,12,1,10", sliced.getElement(1, 12, 1, 3));
        assertEquals("5,3,0,0,13", sliced.getElement(3, 0, 0, 6));
        assertEquals("5,0,2,0,11", sliced.getElement(0, 2, 0, 4));
        assertEquals("5,2,12,0,8", sliced.getElement(2, 12, 0, 1));
    }

    @Test
    @DisplayName("Slicing a tensor is consistent with Numpy, second example")
    void testSlice2() {
        // tensor 2
        int[] dimensions = { 10, 12, 14, 16, 18, 20 };
        List<?> data = generate(dimensions);
        Tensor<String> tensor = Tensor.fromList(data);
        Tensor<String> sliced = tensor.slice(
                Slice.between(3, 7),
                Slice.at(3),
                Slice.to(13),
                Slice.all(),
                Slice.between(3, 4),
                Slice.from(1));
        assertArrayEquals(new int[] { 4, 13, 16, 1, 19 }, sliced.getDimensions());
        assertEquals(15808, sliced.getnEntries());

        //check various slicex
        assertEquals("4,3,11,10,3,2", sliced.getElement(1, 11, 10, 0, 1));
        assertEquals("4,3,2,2,3,14", sliced.getElement(1, 2, 2, 0, 13));
        assertEquals("5,3,8,9,3,9", sliced.getElement(2, 8, 9, 0, 8));
        assertEquals("5,3,0,5,3,12", sliced.getElement(2, 0, 5, 0, 11));
        assertEquals("3,3,0,14,3,14", sliced.getElement(0, 0, 14, 0, 13));
        assertEquals("3,3,9,2,3,10", sliced.getElement(0, 9, 2, 0, 9));
        assertEquals("4,3,4,6,3,10", sliced.getElement(1, 4, 6, 0, 9));
        assertEquals("6,3,8,10,3,15", sliced.getElement(3, 8, 10, 0, 14));
        assertEquals("3,3,1,1,3,14", sliced.getElement(0, 1, 1, 0, 13));
        assertEquals("5,3,10,11,3,10", sliced.getElement(2, 10, 11, 0, 9));
    }

    @Test
    @DisplayName("Slicing a tensor down to a single element works as expected")
    void testSingleElementSlice() {
        int[] dimensions = { 11, 12, 13, 14, 15 };
        List<?> data = generate(dimensions);
        Tensor<String> tensor = Tensor.fromList(data);

        //single element slice
        Tensor<String> singleElement = tensor.slice(
                Slice.at(0),
                Slice.at(0),
                Slice.at(0),
                Slice.at(0),
                Slice.at(0));

        assertEquals("0,0,0,0,0", singleElement.getData()[0]);
        assertArrayEquals(new int[] {}, singleElement.getDimensions());
    }

    @Test
    @DisplayName("Stacking tensors works as expected")
    void testStack() {
        int[] dimensions = { 5, 4, 3 };
        List<Tensor<String>> tensors = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<?> data = generate(dimensions, "stack-" + i);
            tensors.add(Tensor.fromList(data));
        }

        //single element slice
        Tensor<String> stacked = Tensor.stack(tensors.toArray(new Tensor[0]));
        assertArrayEquals(new int[] { 10, 5, 4, 3 }, stacked.getDimensions());
        for (int i = 0; i < 10; i++) {
            assertEquals(tensors.get(i), stacked.slice(Slice.at(i)));
        }
    }

    @Test
    @DisplayName("Concatenating tensors works as expected")
    void testConcatenate() {
        int[] dimensions = { 5, 4, 3 };
        List<Tensor<String>> tensors = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<?> data = generate(dimensions, "stack-" + i);
            tensors.add(Tensor.fromList(data));
        }

        //single element slice
        Tensor<String> stacked = Tensor.concatenate(tensors.toArray(new Tensor[0]));
        assertArrayEquals(new int[] { 50, 4, 3 }, stacked.getDimensions());

        for (int i = 0; i < 10; i++) {
            assertEquals(tensors.get(i), stacked.slice(Slice.between(i * 5, i * 5 + 5)));
        }
    }

    @Test
    @DisplayName("First dimensional slicing shortcut works as expected")
    void testFirstAxisSlice() {
        int[] dimensions = { 5, 40, 30, 20 };
        List<Tensor<String>> tensors = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            List<?> data = generate(dimensions, "stack-" + i);
            tensors.add(Tensor.fromList(data));
        }

        //single element slice
        Tensor<String> stacked = Tensor.stack(tensors.toArray(new Tensor[0]));

        for (int i = 0; i < 10; i++) {
            assertEquals(tensors.get(i), stacked.get(i));
        }
    }

    @Test
    @DisplayName("First dimensional slicing shortcut works as expected")
    void testFirstAxisSliceRange() {
        int[] dimensions = { 5, 40, 30, 20 };
        List<Tensor<String>> tensors = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            List<?> data = generate(dimensions, "stack-" + i);
            tensors.add(Tensor.fromList(data));
        }

        //single element slice
        Tensor<String> stacked = Tensor.stack(tensors.toArray(new Tensor[0]));
        Tensor<String> sliced = Tensor.stack(new Tensor[] { tensors.get(1), tensors.get(3), tensors.get(0), tensors.get(4) });

        assertEquals(sliced, stacked.get(List.of(1, 3, 0, 4)));
    }

    @Test
    @DisplayName("Second dimensional slicing shortcut works as expected")
    void testSecondAxisSlice() {
        int[] dimensions = { 5, 14, 13, 12 };
        List<Tensor<String>> tensors = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            List<Tensor<String>> secondDimTensors = new ArrayList<>();
            for (int ii = 0; ii < 10; ii++) {
                List<?> data = generate(dimensions, "stack-" + i + "," + ii);
                secondDimTensors.add(Tensor.fromList(data));
            }
            tensors.add(Tensor.stack(secondDimTensors.toArray(new Tensor[0])));
        }

        //single element slice
        Tensor<String> stacked = Tensor.stack(tensors.toArray(new Tensor[0]));

        for (int i = 0; i < 10; i++) {
            Tensor<String> t1 = stacked.slice(Slice.all(), Slice.at(i));
            Tensor<String> t2 = stacked.getFromSecondAxis(i);
            assertEquals(t1, t2);
        }
    }

    @Test
    @DisplayName("Tensor mapping works as expected")
    void testMap() {
        int[] dimensions = { 5, 14, 13 };
        Double[][][] data = ArrayGenerators.get3DDoubleArr(dimensions);
        Tensor<Double> tensor = TensorFactory.fromArray(data);

        Tensor<Double> doubled = tensor.map(d -> d * 2);
        Tensor<String> stringed = tensor.map(String::valueOf, new String[tensor.getnEntries()]);

        for (int i = 0; i < tensor.getnEntries(); i++) {
            assertEquals(tensor.getLinearElement(i) * 2, doubled.getLinearElement(i));
            assertEquals(String.valueOf(tensor.getLinearElement(i)), stringed.getLinearElement(i));
        }
    }

    @Test
    @DisplayName("Conversion of tensor to nested list works as expected")
    void testToList() {
        int[] dimensions = { 5, 5, 5, 5 };
        List<?> data = generate(dimensions);
        Tensor<String> tensor = Tensor.fromList(data);
        assertEquals(data, tensor.toNestedList());
    }

    @Test
    @DisplayName("Tensor to string correctly emulates Numpy")
    void testToString() {
        int[] dimensions = { 3, 3, 1 };
        List<?> data = generate(dimensions);
        Tensor<String> tensor = Tensor.fromList(data);
        String expected = "" +
                "tensor([[0,0,0],\n" +
                "        [0,1,0],\n" +
                "        [0,2,0]],\n\n" +
                "       [[1,0,0],\n" +
                "        [1,1,0],\n" +
                "        [1,2,0]],\n\n" +
                "       [[2,0,0],\n" +
                "        [2,1,0],\n" +
                "        [2,2,0]], dtype=java.lang.String)";
        assertEquals(expected, tensor.toString());
    }

    @Test
    @DisplayName("Tensor equality")
    void testToEquals() {
        int[] dimensions = { 5, 5, 5, 5 };
        List<?> data = generate(dimensions);
        Tensor<String> tensor1 = Tensor.fromList(data);
        Tensor<String> tensor2 = Tensor.fromList(data);
        assertEquals(tensor1, tensor2);

        int[] dimensions1 = { 12, 14, 16, 3 };
        data = generate(dimensions1);
        tensor1 = Tensor.fromList(data);
        tensor2 = Tensor.fromList(data);
        assertEquals(tensor1, tensor2);
    }
}
