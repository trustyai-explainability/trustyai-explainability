package org.kie.trustyai.service.payloads.explainability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.kie.trustyai.explainability.model.FeatureImportance;
import org.kie.trustyai.explainability.model.Saliency;
import org.kie.trustyai.explainability.model.SaliencyResults;

public class SaliencyExplanationResponse extends BaseExplanationResponse {

    protected Map<String, List<FeatureSaliency>> saliencies;

    public SaliencyExplanationResponse(Map<String, List<FeatureSaliency>> saliencies) {
        super();
        this.saliencies = saliencies;
    }

    public Map<String, List<FeatureSaliency>> getSaliencies() {
        return saliencies;
    }

    public void setSaliencies(Map<String, List<FeatureSaliency>> saliencies) {
        this.saliencies = saliencies;
    }

    @Override
    public String toString() {
        return "SaliencyExplanationResponse{" +
                "timestamp=" + timestamp +
                ", type='" + type + '\'' +
                ", saliencies=" + saliencies +
                '}';
    }

    public static class FeatureSaliency {

        private String name;
        private Double score;
        private Double confidence;

        public FeatureSaliency() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Double getScore() {
            return score;
        }

        public void setScore(Double score) {
            this.score = score;
        }

        public Double getConfidence() {
            return confidence;
        }

        public void setConfidence(Double confidence) {
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return "FeatureSaliency{" +
                    "name='" + name + '\'' +
                    ", score=" + score +
                    ", confidence=" + confidence +
                    '}';
        }
    }

    public static SaliencyExplanationResponse fromSaliencyResults(@Nonnull SaliencyResults saliencyResults) {
        Map<String, List<SaliencyExplanationResponse.FeatureSaliency>> featureSaliencyMap = new HashMap<>();

        for (Map.Entry<String, Saliency> saliencyMap : saliencyResults.getSaliencies().entrySet()) {
            List<SaliencyExplanationResponse.FeatureSaliency> featureSaliencies = new ArrayList<>();
            String outputName = saliencyMap.getKey();
            Saliency saliency = saliencyMap.getValue();
            for (FeatureImportance featureImportance : saliency.getPerFeatureImportance()) {
                SaliencyExplanationResponse.FeatureSaliency featureSaliency = new SaliencyExplanationResponse.FeatureSaliency();
                featureSaliency.setName(featureImportance.getFeature().getName());
                featureSaliency.setScore(featureImportance.getScore());
                featureSaliency.setConfidence(featureImportance.getConfidence());
                featureSaliencies.add(featureSaliency);
            }
            featureSaliencyMap.put(outputName, featureSaliencies);
        }

        return new SaliencyExplanationResponse(featureSaliencyMap);
    }

    public static SaliencyExplanationResponse empty() {
        Map<String, List<SaliencyExplanationResponse.FeatureSaliency>> featureSaliencyMap = new HashMap<>();
        return new SaliencyExplanationResponse(featureSaliencyMap);
    }
}
