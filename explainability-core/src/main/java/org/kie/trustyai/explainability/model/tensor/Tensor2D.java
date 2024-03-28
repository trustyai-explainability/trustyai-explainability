package org.kie.trustyai.explainability.model.tensor;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class Tensor2D<T> extends BaseTensor<Tensor1D<T>>{
    public Tensor2D(List<Tensor1D<T>> data){
        dimensions = 2;
        validateShapes(data);
        this.data = data;
        this.shape = computeShape(data);
    }

    public Tensor2D() {}

    public static <T> Tensor2D<T> fromLists(List<List<T>> data){
        return new Tensor2D<>(data.stream().map(Tensor1D::new).collect(Collectors.toList()));
    }

    public static <T> Tensor2D<T> fromArray(T[][] array){
        List<Tensor1D<T>> listView = Arrays.stream(array)
                .map(Tensor1D::fromArray)
                .collect(Collectors.toList());
        return new Tensor2D<>(listView);
    }

    @Override
    public T[][] toArray(){
        return (T[][]) data.stream().map(subTensor -> subTensor.toArray()).toArray(Object[][]::new);
    }

    public T get(int i, int ii){
        return data.get(i).get(ii);
    }

    // get slice between [idx1, idx2)
    public Tensor2D<T> slice(int idx1, int idx2){
        return new Tensor2D<>(data.subList(idx1, idx2));
    }


}
