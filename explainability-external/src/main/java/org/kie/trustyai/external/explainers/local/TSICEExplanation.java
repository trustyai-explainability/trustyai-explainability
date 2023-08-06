package org.kie.trustyai.external.explainers.local;

import java.util.List;
import java.util.Map;

public class TSICEExplanation {

    private final Map<String, Map<Object, Object>> dataX;
    private final Object currentForecast;
    private final List<String> featureNames;
    private final List<Object> featureValues;
    private final List<Object> signedImpact;
    private final List<Object> totalImpact;
    private final List<Object> currentFeatureValues;
    private final List<Object> perturbations;

    private final List<Object> forecastOnPerturbations;

    public TSICEExplanation(Map<String, Object> explanation) {
        this.dataX = (Map<String, Map<Object, Object>>) explanation.get("data_x");
        this.currentForecast = explanation.get("current_forecast");
        this.featureNames = (List<String>) explanation.get("feature_names");
        this.featureValues = (List<Object>) explanation.get("feature_values");
        this.signedImpact = (List<Object>) explanation.get("signed_impact");
        this.totalImpact = (List<Object>) explanation.get("total_impact");
        this.currentFeatureValues = (List<Object>) explanation.get("current_feature_values");
        this.perturbations = (List<Object>) explanation.get("perturbations");
        this.forecastOnPerturbations = (List<Object>) explanation.get("forecast_on_perturbations");
    }

    public Map<String, Map<Object, Object>> getDataX() {
        return dataX;
    }

    public Object getCurrentForecast() {
        return currentForecast;
    }

    public List<String> getFeatureNames() {
        return featureNames;
    }

    public List<Object> getFeatureValues() {
        return featureValues;
    }

    public List<Object> getSignedImpact() {
        return signedImpact;
    }

    public List<Object> getTotalImpact() {
        return totalImpact;
    }

    public List<Object> getCurrentFeatureValues() {
        return currentFeatureValues;
    }

    public List<Object> getPerturbations() {
        return perturbations;
    }

    public List<Object> getForecastOnPerturbations() {
        return forecastOnPerturbations;
    }
}
