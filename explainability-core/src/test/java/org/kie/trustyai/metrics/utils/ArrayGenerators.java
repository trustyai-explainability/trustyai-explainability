package org.kie.trustyai.metrics.utils;

public class ArrayGenerators {
    // these functions build an array where the value of the element at position[coords] equals the linear index of those coords
    public static Integer[][] get2DArr(int[] dimension) {
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

    public static Integer[][][] get3DArr(int[] dimension) {
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

    public static Integer[][][][] get4DArr(int[] dimension) {
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

    // these functions build an array where the value of the element at position[coords] equals the linear index of those coords
    public static Double[][] get2DDoubleArr(int[] dimension) {
        int a = dimension[0];
        int b = dimension[1];
        Double[][] arr = new Double[a][b];
        double idx = 0;
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                arr[i][ii] = idx;
                idx += 1;
            }
        }
        return arr;
    }

    public static Double[][][] get3DDoubleArr(int[] dimension) {
        int a = dimension[0];
        int b = dimension[1];
        int c = dimension[2];
        Double[][][] arr = new Double[a][b][c];
        double idx = 0;
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

    public static Double[][][][] get4DDoubleArr(int[] dimension) {
        int a = dimension[0];
        int b = dimension[1];
        int c = dimension[2];
        int d = dimension[3];
        Double[][][][] arr = new Double[a][b][c][d];
        double idx = 0;
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
}
