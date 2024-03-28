package org.kie.trustyai.explainability.model.tensor;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.util.Arrays;
import java.util.List;

@Entity
public class Tensor1D<T> extends BaseTensor<T>{
    public Tensor1D(List<T> data){
        dimensions = 1;
        this.data = data;
        this.shape = List.of(data.size());
    }

    public Tensor1D() {
    }


    public static <T> Tensor1D<T> fromArray(T[] array){
        return new Tensor1D<>(Arrays.asList(array));
    }

    @Override
    public T[] toArray(){
        return (T[]) data.stream().toArray();
    }


    // get slice between [idx1, idx2)
    public Tensor1D<T> slice(int idx1, int idx2){
        return new Tensor1D<>(data.subList(idx1, idx2));
    }
}
