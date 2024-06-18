package org.kie.trustyai.explainability.model.tensor;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tensor1D<T> extends BaseTensor<T, T> {
    public Tensor1D(List<T> data) {
        dimensions = 1;
        this.data = new ArrayList<>(data);
        this.shape = List.of(data.size());
        this.tClass = this.get(0).getClass();
    }

    public static <T> Tensor1D<T> fromArray(T[] array) {
        return new Tensor1D<>(Arrays.asList(array));
    }

    @Override
    public T[] toArray() {
        T[] arr = (T[]) Array.newInstance(getDataClass(), this.length());
        for (int i = 0; i < arr.length; i++) {
            arr[i] = this.data.get(i);
        }
        return arr;
    }

    // get slice between [idx1, idx2)
    public Tensor1D<T> slice(int idx1, int idx2) {
        return new Tensor1D<>(data.subList(idx1, idx2));
    }

    @Override
    public void fill(T val) {
        for (int i = 0; i < data.size(); i++) {
            data.set(i, val);
        }
    }

    @Override
    public Tensor1D<T> copy() {
        return new Tensor1D<>(new ArrayList<>(data));
    }

}
