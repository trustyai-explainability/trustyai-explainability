package org.kie.trustyai.service.payloads.consumer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InferenceLoggerInputV2 {

    private String name;
    private List<Integer> shape;
    private String datatype;

    @JsonProperty("data")
    private List<List<Object>> inputData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public List<List<Object>> getInputData() {
        return inputData;
    }

    public void setInputData(List<List<Object>> inputData) {
        this.inputData = inputData;
    }
}
