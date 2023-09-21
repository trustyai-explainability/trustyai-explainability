package org.kie.trustyai.explainability.model;

import java.time.LocalDateTime;

public class PredictionMetadata {

    private final LocalDateTime predictionTime;

    private final String id;

    private final DatapointSource datapointSource;

    public PredictionMetadata(String id, LocalDateTime predictionTime, DatapointSource datapointSource) {
        this.id = id;
        this.predictionTime = predictionTime;
        this.datapointSource = datapointSource;
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
}
