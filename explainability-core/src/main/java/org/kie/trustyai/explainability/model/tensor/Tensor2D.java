package org.kie.trustyai.explainability.model.tensor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Tensor2D<T> extends BaseTensor<Tensor1D<T>, T> {
    public Tensor2D(List<Tensor1D<T>> data) {
        dimensions = 2;
        validateShapes(data);
        this.data = data;
        this.shape = computeShape(data);
        this.tClass = this.get(0).tClass;
    }

    public Tensor2D() {
    }

    public static <T> Tensor2D<T> fromLists(List<List<T>> data) {
        return new Tensor2D<>(data.stream().map(Tensor1D::new).collect(Collectors.toList()));
    }

    public static <T> Tensor2D<T> fromArray(T[][] array) {
        List<Tensor1D<T>> listView = Arrays.stream(array)
                .map(Tensor1D::fromArray)
                .collect(Collectors.toList());
        return new Tensor2D<>(listView);
    }

    @Override
    public T[][] toArray() {
        T[][] arr = (T[][]) Array.newInstance(getDataClass(), this.shape.get(0), this.shape.get(1));
        for (int i = 0; i < arr.length; i++) {
            arr[i] = this.data.get(i).toArray();
        }
        return arr;
    }

    public T get(int i, int ii) {
        return data.get(i).get(ii);
    }

    // get slice between [idx1, idx2)
    public Tensor2D<T> slice(int idx1, int idx2) {
        return new Tensor2D<>(data.subList(idx1, idx2));
    }

    // fillers
    @Override
    public void fill(T val) {
        for (Tensor1D<T> datum : data) {
            datum.fill(val);
        }
    }

    @Override
    public Tensor2D<T> copy() {
        return new Tensor2D<>(data.stream().map(Tensor1D::copy).collect(Collectors.toList()));
    }
}
