package org.kie.trustyai.explainability.model.tensor;

import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseTensorTest {
    Random rng = new Random(0L);

    // random number generators ====
    public Tensor1D<Double> generate(int i) {
        return new Tensor1D<>(IntStream.range(0, i).mapToDouble(ignored -> rng.nextDouble()).boxed().collect(Collectors.toList()));
    }

    public Tensor2D<Double> generate(int i, int ii) {
        return new Tensor2D<>(IntStream.range(0, i).mapToObj(ignored -> generate(ii)).collect(Collectors.toList()));
    }

    public Tensor3D<Double> generate(int i, int ii, int iii) {
        return new Tensor3D<>(IntStream.range(0, i).mapToObj(ignored -> generate(ii, iii)).collect(Collectors.toList()));
    }

    public Tensor4D<Double> generate(int i, int ii, int iii, int iv) {
        return new Tensor4D<>(IntStream.range(0, i).mapToObj(ignored -> generate(ii, iii, iv)).collect(Collectors.toList()));
    }

    public String getLocator(int idx, String locator) {
        return locator.isEmpty() ? String.valueOf(idx) : locator + "," + idx;
    }

    // sliced ========================================================================================================
    public Tensor1D<String> generatePositionedBetween(int a, int b) {
        return new Tensor1D<>(IntStream.range(a, b).mapToObj(Integer::toString).collect(Collectors.toList()));
    }

    public Tensor2D<String> generatePositionedBetween(int a, int b, int ii) {
        return new Tensor2D<>(IntStream.range(a, b).mapToObj(idx -> generatePositioned(ii, Integer.toString(idx))).collect(Collectors.toList()));
    }

    public Tensor3D<String> generatePositionedBetween(int a, int b, int ii, int iii) {
        return new Tensor3D<>(IntStream.range(a, b).mapToObj(idx -> generatePositioned(ii, iii, Integer.toString(idx))).collect(Collectors.toList()));
    }

    public Tensor4D<String> generatePositionedBetween(int a, int b, int ii, int iii, int iv) {
        return new Tensor4D<>(IntStream.range(a, b).mapToObj(idx -> generatePositioned(ii, iii, iv, Integer.toString(idx))).collect(Collectors.toList()));
    }

    // positionally hinted generators ===============================================================================
    public Tensor1D<String> generatePositioned(int i, String locator) {
        return new Tensor1D<>(IntStream.range(0, i).mapToObj(idx -> getLocator(idx, locator)).collect(Collectors.toList()));
    }

    public Tensor2D<String> generatePositioned(int i, int ii, String locator) {
        return new Tensor2D<>(IntStream.range(0, i).mapToObj(idx -> generatePositioned(ii, getLocator(idx, locator))).collect(Collectors.toList()));
    }

    public Tensor3D<String> generatePositioned(int i, int ii, int iii, String locator) {
        return new Tensor3D<>(IntStream.range(0, i).mapToObj(idx -> generatePositioned(ii, iii, getLocator(idx, locator))).collect(Collectors.toList()));
    }

    public Tensor4D<String> generatePositioned(int i, int ii, int iii, int iv, String locator) {
        return new Tensor4D<>(IntStream.range(0, i).mapToObj(idx -> generatePositioned(ii, iii, iv, getLocator(idx, locator))).collect(Collectors.toList()));
    }

    public Tensor1D<String> generatePositioned(int i) {
        return generatePositioned(i, "");
    }

    public Tensor2D<String> generatePositioned(int i, int ii) {
        return generatePositioned(i, ii, "");
    }

    public Tensor3D<String> generatePositioned(int i, int ii, int iii) {
        return generatePositioned(i, ii, iii, "");
    }

    public Tensor4D<String> generatePositioned(int i, int ii, int iii, int iv) {
        return generatePositioned(i, ii, iii, iv, "");
    }

    // array generators ================================================================================================
    public String[] generatePositionedArray(int i, String locator) {
        String[] out = new String[i];
        IntStream.range(0, i).forEach(idx -> out[idx] = getLocator(idx, locator));
        return out;
    }

    public String[][] generatePositionedArray(int i, int ii, String locator) {
        String[][] out = new String[i][ii];
        IntStream.range(0, i).forEach(idx -> out[idx] = generatePositionedArray(ii, getLocator(idx, locator)));
        return out;
    }

    public String[][][] generatePositionedArray(int i, int ii, int iii, String locator) {
        String[][][] out = new String[i][ii][iii];
        IntStream.range(0, i).forEach(idx -> out[idx] = generatePositionedArray(ii, iii, getLocator(idx, locator)));
        return out;
    }

    public String[][][][] generatePositionedArray(int i, int ii, int iii, int iv, String locator) {
        String[][][][] out = new String[i][ii][iii][iv];
        IntStream.range(0, i).forEach(idx -> out[idx] = generatePositionedArray(ii, iii, iv, getLocator(idx, locator)));
        return out;
    }

    // get tests ==============================================================================================================
    @Test
    void testGet1D() {
        Tensor1D<String> tensor = generatePositioned(10);
        assertEquals("5", tensor.get(5));
        assertEquals("7", tensor.get(7));
    }

    @Test
    void testGet2D() {
        int a = 10;
        int b = 20;

        Tensor2D<String> tensor = generatePositioned(a, b);
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                String position = i + "," + ii;
                assertEquals(position, tensor.get(i, ii));
                assertEquals(position, tensor.get(i).get(ii));
            }
        }
    }

    @Test
    void testGet3D() {
        int a = 10;
        int b = 20;
        int c = 5;

        Tensor3D<String> tensor = generatePositioned(a, b, c);
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                for (int iii = 0; iii < c; iii++) {
                    String position = i + "," + ii + "," + iii;
                    assertEquals(position, tensor.get(i, ii, iii));
                    assertEquals(position, tensor.get(i, ii).get(iii));
                    assertEquals(position, tensor.get(i).get(ii).get(iii));
                }
            }
        }
    }

    @Test
    void testGet4D() {
        int a = 10;
        int b = 20;
        int c = 5;
        int d = 7;

        Tensor4D<String> tensor = generatePositioned(a, b, c, d);
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                for (int iii = 0; iii < c; iii++) {
                    for (int iv = 0; iv < c; iv++) {
                        String position = i + "," + ii + "," + iii + "," + iv;
                        assertEquals(position, tensor.get(i, ii, iii, iv));
                        assertEquals(position, tensor.get(i, ii, iii).get(iv));
                        assertEquals(position, tensor.get(i, ii).get(iii).get(iv));
                        assertEquals(position, tensor.get(i, ii).get(iii, iv));
                        assertEquals(position, tensor.get(i).get(ii).get(iii).get(iv));
                    }
                }
            }
        }
    }

    // slice tests ========================================================================================
    @Test
    void testSlice1D() {
        Tensor1D<String> tensor = generatePositioned(10);
        Tensor1D<String> tensorSliced = generatePositionedBetween(5, 7);
        assertArrayEquals(tensorSliced.toArray(), tensor.slice(5, 7).toArray());
    }

    @Test
    void testSlice2D() {
        Tensor2D<String> tensor = generatePositioned(10, 10);
        Tensor2D<String> tensorSliced = generatePositionedBetween(5, 7, 10);
        assertArrayEquals(tensorSliced.toArray(), tensor.slice(5, 7).toArray());
    }

    @Test
    void testSlice3D() {
        Tensor3D<String> tensor = generatePositioned(10, 10, 10);
        Tensor3D<String> tensorSliced = generatePositionedBetween(5, 7, 10, 10);
        assertArrayEquals(tensorSliced.toArray(), tensor.slice(5, 7).toArray());
    }

    @Test
    void testSlice4D() {
        Tensor4D<String> tensor = generatePositioned(10, 10, 10, 10);
        Tensor4D<String> tensorSliced = generatePositionedBetween(5, 7, 10, 10, 10);
        assertArrayEquals(tensorSliced.toArray(), tensor.slice(5, 7).toArray());
    }

    // toArray tests ========================================================================================
    @Test
    void testToArray1D() {
        Tensor1D<String> tensor = generatePositioned(10);
        String[] array = generatePositionedArray(10, "");
        assertArrayEquals(array, tensor.toArray());
    }

    @Test
    void testToArray2D() {
        Tensor2D<String> tensor = generatePositioned(10, 11);
        String[][] array = generatePositionedArray(10, 11, "");
        assertArrayEquals(array, tensor.toArray());
    }

    @Test
    void testToArray3D() {
        Tensor3D<String> tensor = generatePositioned(10, 11, 12);
        String[][][] array = generatePositionedArray(10, 11, 12, "");
        assertArrayEquals(array, tensor.toArray());
    }

    @Test
    void testToArray4D() {
        Tensor4D<String> tensor = generatePositioned(10, 11, 12, 13);
        String[][][][] array = generatePositionedArray(10, 11, 12, 13, "");
        assertArrayEquals(array, tensor.toArray());
    }

    // fromArray tests ========================================================================================
    @Test
    void testFromArray1D() {
        Tensor1D<String> expected = generatePositioned(10);
        Tensor1D<String> actual = Tensor1D.fromArray(generatePositionedArray(10, ""));
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testFromArray2D() {
        Tensor2D<String> expected = generatePositioned(10, 11);
        Tensor2D<String> actual = Tensor2D.fromArray(generatePositionedArray(10, 11, ""));
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testFromArray3D() {
        Tensor3D<String> expected = generatePositioned(10, 11, 12);
        Tensor3D<String> actual = Tensor3D.fromArray(generatePositionedArray(10, 11, 12, ""));
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void testFromArray4D() {
        Tensor4D<String> expected = generatePositioned(10, 11, 12, 13);
        Tensor4D<String> actual = Tensor4D.fromArray(generatePositionedArray(10, 11, 12, 13, ""));
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    // fromArray tests ========================================================================================
    @Test
    void testEqual1D() {
        Tensor1D<String> expected = generatePositioned(10);
        Tensor1D<String> actual = generatePositioned(10);
        Tensor1D<String> actualInequal = generatePositioned(10, "inequal");
        assertEquals(expected, actual);
        assertNotEquals(expected, actualInequal);
    }

    @Test
    void testEqual2D() {
        Tensor2D<String> expected = generatePositioned(10, 11);
        Tensor2D<String> actual = generatePositioned(10, 11);
        Tensor2D<String> actualInequal = generatePositioned(10, 11, "inequal");
        assertEquals(expected, actual);
        assertNotEquals(expected, actualInequal);
    }

    @Test
    void testEqual3D() {
        Tensor3D<String> expected = generatePositioned(10, 11, 12);
        Tensor3D<String> actual = generatePositioned(10, 11, 12);
        Tensor3D<String> actualInequal = generatePositioned(10, 11, 12, "inequal");
        assertEquals(expected, actual);
        assertNotEquals(expected, actualInequal);
    }

    @Test
    void testEqual4D() {
        Tensor4D<String> expected = generatePositioned(10, 11, 12, 13);
        Tensor4D<String> actual = generatePositioned(10, 11, 12, 13);
        Tensor4D<String> actualInequal = generatePositioned(10, 11, 12, 13, "inequal");
        assertEquals(expected, actual);
        assertNotEquals(expected, actualInequal);
    }

    @Test
    void testFill4D() {
        Tensor4D<String> expected = generatePositioned(10, 11, 12, 13);
        expected.fill("");

        // check some random paths through array
        String[][][][] arr = expected.toArray();
        for (int i = 0; i < 10; i++) {
            assertEquals("", arr[i][i][i][i]);
            assertEquals("", arr[i][Math.max(0, i - 1)][i][Math.max(0, i - 1)]);
            assertEquals("", arr[i][Math.max(0, i - 2)][Math.max(0, i - 2)][Math.max(0, i - 3)]);
        }
    }

}
