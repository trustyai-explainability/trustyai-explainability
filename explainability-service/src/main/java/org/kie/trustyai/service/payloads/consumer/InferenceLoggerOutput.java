package org.kie.trustyai.service.payloads.consumer;

import java.util.List;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.UnderlyingObject;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.*;

@Entity
public class InferenceLoggerOutput {
    // Match the old "predictions" format
    @JsonAlias({ "predictions" })
    @ElementCollection(fetch = FetchType.EAGER)
    private List<UnderlyingObject> predictions;

    // Match the "outputs" format
    @JsonProperty("outputs")
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<InferenceLoggerOutputObject> outputs;

    @Column(name = "inference_id")
    private String id;

    @Id
    @GeneratedValue
    @Column(name = "id")
    private Long dbId;

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }

    public Long getDbId() {
        return dbId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public List<UnderlyingObject> getPredictions() {
        if (predictions != null) {
            return predictions;
        } else if (outputs != null && !outputs.isEmpty()) {
            return outputs.get(0).getData();
        }
        return null;
    }

    public List<InferenceLoggerOutputObject> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<InferenceLoggerOutputObject> outputs) {
        this.outputs = outputs;
    }

    public List<UnderlyingObject> getRawPredictions() {
        return this.predictions;
    }

    public void setPredictions(List<UnderlyingObject> predictions) {
        this.predictions = predictions;
    }

    public void setPredictionsDouble(List<Double> predictions) {
        this.predictions = predictions.stream().map(UnderlyingObject::new).collect(Collectors.toList());
    }

}
