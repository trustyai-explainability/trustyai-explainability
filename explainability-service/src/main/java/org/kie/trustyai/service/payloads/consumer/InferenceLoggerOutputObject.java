package org.kie.trustyai.service.payloads.consumer;

import java.util.List;

import org.kie.trustyai.explainability.model.SerializableObject;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class InferenceLoggerOutputObject {
    private String name;

    @Id
    @GeneratedValue
    private Long id;

    @ElementCollection(fetch = FetchType.EAGER)
    public List<Integer> getShape() {
        return shape;
    }

    public void setShape(List<Integer> shape) {
        this.shape = shape;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public List<SerializableObject> getData() {
        return data;
    }

    public void setData(List<SerializableObject> data) {
        this.data = data;
    }

    private List<Integer> shape;
    private String datatype;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<SerializableObject> data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
