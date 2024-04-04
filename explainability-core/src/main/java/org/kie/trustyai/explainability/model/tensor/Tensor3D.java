package org.kie.trustyai.explainability.model.tensor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Tensor3D<T> extends BaseTensor<Tensor2D<T>, T> {

    public Tensor3D(List<Tensor2D<T>> data) {
        dimensions = 3;
        validateShapes(data);
        this.data = data;
        this.shape = computeShape(data);
        this.tClass = this.get(0).tClass;
    }

    public Tensor3D() {
    }

    public static <T> Tensor3D<T> fromLists(List<List<List<T>>> data) {
        return new Tensor3D<>(data.stream().map(Tensor2D::fromLists).collect(Collectors.toList()));
    }

    public static <T> Tensor3D<T> fromArray(T[][][] array) {
        List<Tensor2D<T>> listView = Arrays.stream(array)
                .map(Tensor2D::fromArray)
                .collect(Collectors.toList());
        return new Tensor3D<>(listView);
    }

    @Override
    public T[][][] toArray() {
        T[][][] arr = (T[][][]) Array.newInstance(getDataClass(), this.shape.get(0), this.shape.get(1), this.shape.get(2));
        for (int i = 0; i < arr.length; i++) {
            arr[i] = this.data.get(i).toArray();
        }
        return arr;
    }

    // getters =========================================================================================================
    public Tensor1D<T> get(int i, int ii) {
        return data.get(i).get(ii);
    }

    public T get(int i, int ii, int iii) {
        return data.get(i).get(ii).get(iii);
    }

    // get slice between [idx1, idx2)
    public Tensor3D<T> slice(int idx1, int idx2) {
        return new Tensor3D<>(data.subList(idx1, idx2));
    }

    @Override
    public void fill(T val) {
        for (Tensor2D<T> datum : data) {
            datum.fill(val);
        }
    }

    @Override
    public Tensor3D<T> copy() {
        return new Tensor3D<>(data.stream().map(Tensor2D::copy).collect(Collectors.toList()));
    }
}
