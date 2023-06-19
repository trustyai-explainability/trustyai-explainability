package org.kie.trustyai.explainability.model;

import java.time.LocalDateTime;

public class PredictionMetadata {

    private final LocalDateTime predictionTime;

    private final String generatedBy;

    private final String id;

    private final boolean synthetic;

    public PredictionMetadata(String id, String generatedBy, LocalDateTime predictionTime, boolean synthetic) {
        this.id = id;
        this.generatedBy = generatedBy;
        this.predictionTime = predictionTime;
        this.synthetic = synthetic;
    }

    public String getId() {
        return id;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public LocalDateTime getPredictionTime() {
        return predictionTime;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
