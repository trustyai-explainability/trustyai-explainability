package org.kie.trustyai.service.payloads.consumer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class InferenceLoggerOutput {
    // Match the old "predictions" format
    @JsonAlias({ "predictions" })
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Double> predictions;

    // Match the "outputs" format
    @JsonProperty("outputs")
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<InferenceLoggerOutputObject> outputs;

    @Id
    @GeneratedValue
    private Long id;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public List<Double> getPredictions() {
        if (predictions != null) {
            return predictions;
        } else if (outputs != null && !outputs.isEmpty()) {
            return outputs.get(0).getData();
        }
        return null;
    }

    public void setPredictions(List<Double> predictions) {
        this.predictions = predictions;
    }
}
