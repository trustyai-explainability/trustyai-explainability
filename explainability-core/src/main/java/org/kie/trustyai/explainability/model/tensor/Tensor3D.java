package org.kie.trustyai.explainability.model.tensor;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
public class Tensor3D<T> extends BaseTensor<Tensor2D<T>>{

    public Tensor3D(List<Tensor2D<T>> data){
        dimensions = 3;
        validateShapes(data);
        this.data = data;
        this.shape = computeShape(data);
    }

    public Tensor3D() {}

    public static <T> Tensor3D<T> fromLists(List<List<List<T>>> data){
        return new Tensor3D<>(data.stream().map(Tensor2D::fromLists).collect(Collectors.toList()));
    }

    public static <T> Tensor3D<T> fromArray(T[][][] array){
        List<Tensor2D<T>> listView = Arrays.stream(array)
                .map(Tensor2D::fromArray)
                .collect(Collectors.toList());
        return new Tensor3D<>(listView);
    }

    @Override
    public T[][][] toArray(){
        return (T[][][]) data.stream().map(subTensor -> subTensor.toArray()).toArray(Object[][][]::new);
    }


    // getters =========================================================================================================
    public Tensor1D<T> get(int i, int ii){
        return data.get(i).get(ii);
    }

    public T get(int i, int ii, int iii){
        return data.get(i).get(ii).get(iii);
    }

    // get slice between [idx1, idx2)
    public Tensor3D<T> slice(int idx1, int idx2){
        return new Tensor3D<>(data.subList(idx1, idx2));
    }
}
