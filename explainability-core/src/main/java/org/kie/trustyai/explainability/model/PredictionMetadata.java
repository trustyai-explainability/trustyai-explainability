package org.kie.trustyai.explainability.model;

import java.time.LocalDateTime;

public class PredictionMetadata {

    private final LocalDateTime predictionTime;

    private final String id;

    private final DatapointSource datapointSource;

    private final Value groundTruth;

    public PredictionMetadata(String id, LocalDateTime predictionTime, DatapointSource datapointSource) {
        this.id = id;
        this.predictionTime = predictionTime;
        this.datapointSource = datapointSource;
        this.groundTruth = null;
    }

    public PredictionMetadata(String id, LocalDateTime predictionTime, DatapointSource datapointSource, Value groundTruth) {
        this.id = id;
        this.predictionTime = predictionTime;
        this.datapointSource = datapointSource;
        this.groundTruth = groundTruth;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getPredictionTime() {
        return predictionTime;
    }

    public DatapointSource getDataPointSource() {
        return datapointSource;
    }

    public Value getGroundTruth() {
        return groundTruth;
    }
}
