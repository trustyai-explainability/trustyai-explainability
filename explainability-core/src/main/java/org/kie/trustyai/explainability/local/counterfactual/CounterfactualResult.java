/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.trustyai.explainability.local.counterfactual;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.Pair;
import org.kie.trustyai.explainability.local.counterfactual.entities.CounterfactualEntity;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.utils.IOUtils;

/**
 * Represents the result of a counterfactual search.
 * Entities represent the counterfactual features and the {@link PredictionOutput}
 * contains the prediction result for the counterfactual, including the prediction score, if available.
 */
public class CounterfactualResult {

    private List<CounterfactualEntity> entities;
    private List<PredictionOutput> output;
    private List<Feature> features;
    private boolean valid;

    private UUID solutionId;
    private UUID executionId;
    private long sequenceId;

    public CounterfactualResult(List<CounterfactualEntity> entities,
            List<Feature> features,
            List<PredictionOutput> output,
            boolean valid,
            UUID solutionId,
            UUID executionId,
            long sequenceId) {
        this.entities = entities;
        this.features = features;
        this.output = output;
        this.valid = valid;
        this.solutionId = solutionId;
        this.executionId = executionId;
        this.sequenceId = sequenceId;
    }

    public List<Feature> getFeatures() {
        return features;
    }

    public List<CounterfactualEntity> getEntities() {
        return entities;
    }

    public List<PredictionOutput> getOutput() {
        return output;
    }

    public boolean isValid() {
        return valid;
    }

    public UUID getSolutionId() {
        return solutionId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public long getSequenceId() {
        return sequenceId;
    }

    public String toString(List<Output> originalOutputs, List<Output> goal){
        return toString(3, originalOutputs, goal);
    }
    /**

    /**
     * Represent the counterfactual result as a string
     *
     * @param decimalPlaces The amount of decimals to round the numeric values to
     * @param originalOutputs The list of original outputs of the model given the original input
     * @param goal The counterfactual goal
     * @return CounterfactualResult string
     */
    public String toString(int decimalPlaces, List<Output> originalOutputs, List<Output> goal){
        List<Feature> newFeatures = this.getEntities().stream().map(CounterfactualEntity::asFeature).collect(Collectors.toList());
        List<String> featureNames = new ArrayList<>(List.of("Feature"));
        List<String> featureDomains = new ArrayList<>(List.of("Domain"));
        List<String> featureValues = new ArrayList<>(List.of("Found Value"));
        List<String> originalFeatureValues = new ArrayList<>(List.of("Original Value"));
        for (int i=0; i< newFeatures.size(); i++){
            featureNames.add(newFeatures.get(i).getName());
            featureDomains.add(this.features.get(i).getDomain().toString());
            featureValues.add(IOUtils.roundedString(newFeatures.get(i), decimalPlaces));
            originalFeatureValues.add(IOUtils.roundedString(features.get(i), decimalPlaces));
        }

        // Outputs
        List<Output> newOutputs = output.get(0).getOutputs();
        List<String> goalOutputs = new ArrayList<>(List.of("Goal"));
        List<String> outputNames = new ArrayList<>(List.of("Output"));
        List<String> originalOutputValues = new ArrayList<>(List.of("Original Value"));
        List<String> outputValues = new ArrayList<>(List.of("Found Value"));

        for (int i=0; i<newOutputs.size(); i++){
            outputNames.add(newOutputs.get(i).getName());
            goalOutputs.add(IOUtils.roundedString(goal.get(i), decimalPlaces));
            originalOutputValues.add(IOUtils.roundedString(originalOutputs.get(i), decimalPlaces));
            outputValues.add(IOUtils.roundedString(newOutputs.get(i), decimalPlaces));
        }
        int featureTableLength = featureNames.size();
        // join feature and outputs into single table
        featureNames.addAll(outputNames);
        featureDomains.addAll(goalOutputs);
        originalFeatureValues.addAll(originalOutputValues);
        featureValues.addAll(outputValues);

        Pair<String, Integer> tableAndWidth = IOUtils.generateTable(
                List.of("Features", "Outputs"),
                List.of(0, featureTableLength),
                List.of(),
                List.of(featureNames, featureDomains, originalFeatureValues, featureValues),
                List.of(" |", " | ", "  →"));

        StringBuilder out = new StringBuilder();
        out.append("=== Counterfactual Search Results ");
        out.append(StringUtils.repeat("=", tableAndWidth.getSecond()-34)).append(String.format("%n"));
        out.append(tableAndWidth.getFirst()).append(String.format("%n"));
        out.append("Meets Validity Criteria? ").append(this.isValid()).append(String.format("%n"));
        out.append(StringUtils.repeat("=", tableAndWidth.getSecond()));
        return out.toString();
    }
}
