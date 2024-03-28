package org.kie.trustyai.explainability.model.tensor;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@MappedSuperclass
public class BaseTensor<E> {
    int dimensions;

    @OneToMany
    List<E> data;

    @ElementCollection
    List<Integer> shape;
    private Long id;


    // default constructor
    public BaseTensor(){}

    // constructor from list of subelements
    public BaseTensor(List<E> data){
        this.data = data;
    }

    // hibernate ids
    @Id
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // tensor info
    public List<Integer> getShape(){
        return shape;
    }

    public int getDimensions(){
        return dimensions;
    }

    public int length(){
        return data.size();
    }

    // get element at position idx
    public E get(int idx){
        return data.get(idx);
    }


    public Object[] toArray(){
        return null;
    }

    @Override
    public String toString() {
        return Arrays.deepToString(toArray());
    }

    // shape processing
    protected <E extends BaseTensor> List<Integer> computeShape(List<E> data){
        List<Integer> shape = new ArrayList<Integer>(data.get(0).getShape());
        shape.add(0, data.size());
        return Collections.unmodifiableList(shape);
    }

    //check that all shapes are of a consistent size
    protected static <E extends BaseTensor> void validateShapes(List<E> data) throws IllegalArgumentException {
        Set<Integer> shapes = data.stream().mapToInt(BaseTensor::length).boxed().collect(Collectors.toSet());
        if (shapes.size() != 1){
            throw new IllegalArgumentException("All slices of a tensor dimension must have the same shape: found the following sizes: " + shapes);
        }
    }
}
