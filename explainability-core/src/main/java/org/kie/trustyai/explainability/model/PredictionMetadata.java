package org.kie.trustyai.explainability.model;

import java.time.LocalDateTime;

public class PredictionMetadata {

    private final LocalDateTime predictionTime;

    private final String id;

    private final boolean synthetic;

    public PredictionMetadata(String id, LocalDateTime predictionTime, boolean synthetic) {
        this.id = id;
        this.predictionTime = predictionTime;
        this.synthetic = synthetic;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getPredictionTime() {
        return predictionTime;
    }

    public boolean isSynthetic() {
        return synthetic;
    }
}
