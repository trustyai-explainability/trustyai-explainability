package org.kie.trustyai.explainability.model.tensor;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class Tensor4D<T> extends BaseTensor<Tensor3D<T>>{

    public Tensor4D(List<Tensor3D<T>> data){
        dimensions = 4;
        validateShapes(data);
        this.data = data;
        this.shape = computeShape(data);
    }

    public Tensor4D() {}

    public static <T> Tensor4D<T> fromLists(List<List<List<List<T>>>> data){
        return new Tensor4D<>(data.stream().map(Tensor3D::fromLists).collect(Collectors.toList()));
    }

    public static <T> Tensor4D<T> fromArray(T[][][][] array){
        List<Tensor3D<T>> listView = Arrays.stream(array)
                .map(Tensor3D::fromArray)
                .collect(Collectors.toList());
        return new Tensor4D<>(listView);
    }

    @Override
    public T[][][][] toArray(){
        return (T[][][][]) data.stream().map(subTensor -> subTensor.toArray()).toArray(Object[][][][]::new);
    }

    // getters =========================================================================================================
    public Tensor2D<T> get(int i, int ii){
        return data.get(i).get(ii);
    }

    public Tensor1D<T> get(int i, int ii, int iii){
        return data.get(i).get(ii).get(iii);
    }

    public T get(int i, int ii, int iii, int iv){
        return data.get(i).get(ii).get(iii).get(iv);
    }

    public Tensor4D<T> slice(int idx1, int idx2){
        return new Tensor4D<>(data.subList(idx1, idx2));
    }

}
