package org.kie.trustyai.explainability.model.tensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class BaseTensor<E, T> implements Serializable {
    int dimensions;

    protected Class tClass;
    List<E> data;
    List<Integer> shape;

    // constructor from list of subelements
    protected BaseTensor() {
    }

    // tensor info
    public List<Integer> getShape() {
        return shape;
    }

    public int getDimensions() {
        return dimensions;
    }

    public int length() {
        return data.size();
    }

    public Class getDataClass() {
        return tClass;
    }

    // get element at position idx
    public E get(int idx) {
        return data.get(idx);
    }

    // fill with value
    public abstract void fill(T val);

    public abstract Object[] toArray();

    public String toString() {
        return Arrays.deepToString(toArray());
    }

    // copy
    public abstract <E extends BaseTensor> E copy();

    // shape processing
    protected <E extends BaseTensor> List<Integer> computeShape(List<E> data) {
        List<Integer> shape = new ArrayList<Integer>(data.get(0).getShape());
        shape.add(0, data.size());
        return Collections.unmodifiableList(shape);
    }

    //check that all shapes are of a consistent size
    protected static <E extends BaseTensor> void validateShapes(List<E> data) throws IllegalArgumentException {
        Set<Integer> shapes = data.stream().mapToInt(BaseTensor::length).boxed().collect(Collectors.toSet());
        if (shapes.size() != 1) {
            throw new IllegalArgumentException("All slices of a tensor dimension must have the same shape: found the following sizes: " + shapes);
        }
    }

    // equality checks
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BaseTensor))
            return false;
        BaseTensor<?, ?> that = (BaseTensor<?, ?>) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
