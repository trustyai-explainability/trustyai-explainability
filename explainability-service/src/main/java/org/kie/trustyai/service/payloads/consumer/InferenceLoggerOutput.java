package org.kie.trustyai.service.payloads.consumer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;

@Embeddable
public class InferenceLoggerOutput {
    // Match the old "predictions" format
    @JsonAlias({ "predictions" })
    private List<Double> predictions;

    // Match the "outputs" format
    @JsonProperty("outputs")
    private List<Output> outputs;

    public static class Output {
        private String name;

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

        public List<Double> getData() {
            return data;
        }

        public void setData(List<Double> data) {
            this.data = data;
        }

        private List<Integer> shape;
        private String datatype;
        private List<Double> data;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Transient
    public List<Double> getPredictions() {
        if (predictions != null) {
            return predictions;
        } else if (outputs != null && !outputs.isEmpty()) {
            return outputs.get(0).data;
        }
        return null;
    }

    public void setPredictions(List<Double> predictions) {
        this.predictions = predictions;
    }
}
