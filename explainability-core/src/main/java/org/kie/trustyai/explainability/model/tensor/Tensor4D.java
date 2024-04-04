package org.kie.trustyai.explainability.model.tensor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Tensor4D<T> extends BaseTensor<Tensor3D<T>, T> {

    public Tensor4D(List<Tensor3D<T>> data) {
        dimensions = 4;
        validateShapes(data);
        this.data = data;
        this.shape = computeShape(data);
        this.tClass = this.get(0).tClass;
    }

    public Tensor4D() {
    }

    public static <T> Tensor4D<T> fromLists(List<List<List<List<T>>>> data) {
        return new Tensor4D<>(data.stream().map(Tensor3D::fromLists).collect(Collectors.toList()));
    }

    public static <T> Tensor4D<T> fromArray(T[][][][] array) {
        List<Tensor3D<T>> listView = Arrays.stream(array)
                .map(Tensor3D::fromArray)
                .collect(Collectors.toList());
        return new Tensor4D<>(listView);
    }

    @Override
    public T[][][][] toArray() {
        T[][][][] arr = (T[][][][]) Array.newInstance(getDataClass(), this.shape.get(0), this.shape.get(1), this.shape.get(2), this.shape.get(3));
        for (int i = 0; i < arr.length; i++) {
            arr[i] = this.data.get(i).toArray();
        }
        return arr;
    }

    // getters =========================================================================================================
    public Tensor2D<T> get(int i, int ii) {
        return data.get(i).get(ii);
    }

    public Tensor1D<T> get(int i, int ii, int iii) {
        return data.get(i).get(ii).get(iii);
    }

    public T get(int i, int ii, int iii, int iv) {
        return data.get(i).get(ii).get(iii).get(iv);
    }

    public Tensor4D<T> slice(int idx1, int idx2) {
        return new Tensor4D<>(data.subList(idx1, idx2));
    }

    @Override
    public void fill(T val) {
        for (Tensor3D<T> datum : data) {
            datum.fill(val);
        }
    }

    @Override
    public Tensor4D<T> copy() {
        return new Tensor4D<>(data.stream().map(Tensor3D::copy).collect(Collectors.toList()));
    }
}
