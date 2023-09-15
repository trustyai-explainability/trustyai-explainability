package org.kie.trustyai.explainability.model;

import java.time.LocalDateTime;

public class PredictionMetadata {

    private final LocalDateTime predictionTime;

    private final String id;

    private final String datapointTag;

    private final Value groundTruth;

    public PredictionMetadata(String id, LocalDateTime predictionTime) {
        this.id = id;
        this.predictionTime = predictionTime;
        this.datapointTag = "";
        this.groundTruth = null;
    }

    public PredictionMetadata(String id, LocalDateTime predictionTime, String datapointTag) {
        this.id = id;
        this.predictionTime = predictionTime;
        this.datapointTag = datapointTag;
        this.groundTruth = null;
    }

    public PredictionMetadata(String id, LocalDateTime predictionTime, String datapointTag, Value groundTruth) {
        this.id = id;
        this.predictionTime = predictionTime;
        this.datapointTag = datapointTag;
        this.groundTruth = groundTruth;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getPredictionTime() {
        return predictionTime;
    }

    public String getDataPointTag() {
        return datapointTag;
    }

    public Value getGroundTruth() {
        return groundTruth;
    }
}
