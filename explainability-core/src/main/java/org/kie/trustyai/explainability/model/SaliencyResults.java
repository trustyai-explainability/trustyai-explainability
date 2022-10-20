/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
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

package org.kie.trustyai.explainability.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.kie.trustyai.explainability.utils.IOUtils;

public class SaliencyResults {
    private final Map<String, Saliency> saliencies;

    private final SourceExplainer sourceExplainer;

    public enum SourceExplainer {
        SHAP,
        LIME,
        AGGREGATED_LIME
    }

    public SaliencyResults(Map<String, Saliency> saliencies, SourceExplainer sourceExplainer) {
        this.saliencies = saliencies;
        this.sourceExplainer = sourceExplainer;
    }

    public Map<String, Saliency> getSaliencies() {
        return saliencies;
    }

    public SourceExplainer getSourceExplainer() {
        return sourceExplainer;
    }

    public SaliencyResults difference(SaliencyResults other) {
        if (!this.sourceExplainer.equals(other.sourceExplainer)) {
            throw new IllegalArgumentException(
                    String.format(
                            "Compared SaliencyResults must come from the same source explainer, got %s and %s.",
                            this.sourceExplainer, other.sourceExplainer));
        }
        ;

        Map<String, Saliency> condensedSaliencies = new HashMap<>();
        for (Map.Entry<String, Saliency> saliencyEntry : this.saliencies.entrySet()) {
            Saliency s0 = this.getSaliencies().get(saliencyEntry.getKey());
            Saliency s1 = other.getSaliencies().get(saliencyEntry.getKey());

            List<FeatureImportance> pfi0 = s0.getPerFeatureImportance();
            List<FeatureImportance> pfi1 = s1.getPerFeatureImportance();
            List<FeatureImportance> condensedFeatureImportances = new ArrayList<>();

            for (int fiIDX = 0; fiIDX < pfi0.size(); fiIDX++) {
                Feature condenseF = new Feature(
                        pfi0.get(fiIDX).getFeature().getName(),
                        Type.TEXT,
                        new Value(pfi0.get(fiIDX).getFeature().getValue().toString()
                                + " -> " + pfi1.get(fiIDX).getFeature().getValue().toString()));
                condensedFeatureImportances.add(new FeatureImportance(condenseF,
                        pfi0.get(fiIDX).getScore() - pfi1.get(fiIDX).getScore(),
                        Math.sqrt(Math.pow(pfi0.get(fiIDX).getConfidence(), 2)
                                + Math.pow(pfi1.get(fiIDX).getConfidence(), 2))));
            }
            Output condensedOutput = new Output(
                    s0.getOutput().getName(),
                    Type.TEXT,
                    new Value(s0.getOutput().getValue().asNumber() - s1.getOutput().getValue().asNumber()),
                    (s0.getOutput().getScore() + s1.getOutput().getScore()) / 2);
            condensedSaliencies.put(saliencyEntry.getKey(), new Saliency(condensedOutput, condensedFeatureImportances));
        }

        return new SaliencyResults(condensedSaliencies, this.sourceExplainer);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SaliencyResults other = (SaliencyResults) o;
        if (this.saliencies.size() != other.getSaliencies().size()) {
            return false;
        }
        if (!this.sourceExplainer.equals(other.sourceExplainer)) {
            return false;
        }

        for (Map.Entry<String, Saliency> entry : this.saliencies.entrySet()) {
            if (other.getSaliencies().containsKey(entry.getKey())) {
                List<FeatureImportance> thisPFIs = this.saliencies.get(entry.getKey()).getPerFeatureImportance();
                List<FeatureImportance> otherPFIs = other.getSaliencies().get(entry.getKey()).getPerFeatureImportance();
                if (thisPFIs.size() != otherPFIs.size()) {
                    return false;
                }
                for (int j = 0; j < thisPFIs.size(); j++) {
                    if (!thisPFIs.get(j).equals(otherPFIs.get(j))) {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(saliencies.hashCode());
    }

    /**
     * Represent the SaliencyResult as a string
     *
     * @return SaliencyResult string
     */
    public String asTable() {
        return asTable(3);
    }

    /**
     * Represent the SaliencyResult as a string
     *
     * @param decimalPlaces The decimal places to round all numeric values in the table to
     * @return SaliencyResult string
     */
    public String asTable(int decimalPlaces) {
        List<String> featureNames = new ArrayList<>();
        List<String> featureValues = new ArrayList<>();
        List<String> saliencyValues = new ArrayList<>();
        List<String> confidences = new ArrayList<>();

        String saliencyHeader;
        String pluralSaliencyHeader;
        if (this.sourceExplainer.equals(SourceExplainer.SHAP)) {
            saliencyHeader = "SHAP Value";
            pluralSaliencyHeader = "SHAP Values";
        } else {
            saliencyHeader = "Saliency";
            pluralSaliencyHeader = "LIME Saliencies";
        }

        List<String> headers = new ArrayList<>();
        List<Integer> headerPositions = new ArrayList<>();
        List<Integer> lineSeparatorPositions = new ArrayList<>();
        int lineIDX = 0;

        for (Map.Entry<String, Saliency> entry : saliencies.entrySet()) {
            Saliency saliency = entry.getValue();
            List<FeatureImportance> pfis = saliency.getPerFeatureImportance();
            headers.add(entry.getKey() + " " + pluralSaliencyHeader);
            headerPositions.add(lineIDX);

            featureNames.add("Feature");
            featureValues.add("Value");
            saliencyValues.add(saliencyHeader);
            confidences.add(" | Confidence");
            lineIDX++;

            if (sourceExplainer.equals(SourceExplainer.SHAP)) {
                featureNames.add("");
                featureValues.add("FNull");
                saliencyValues.add(IOUtils.roundedString(pfis.get(pfis.size() - 1).getScore(), decimalPlaces));
                confidences.add("");
                lineIDX++;
            }

            for (int i = 0; i < pfis.size() - 1; i++) {
                featureNames.add(pfis.get(i).getFeature().getName() + " = ");
                featureValues.add(IOUtils.roundedString(pfis.get(i).getFeature(), decimalPlaces));
                saliencyValues.add(IOUtils.roundedString(pfis.get(i).getScore(), decimalPlaces));
                confidences.add(IOUtils.roundedString(pfis.get(i).getConfidence(), decimalPlaces));
                lineIDX++;
            }

            lineSeparatorPositions.add(lineIDX);
            featureNames.add("");
            featureValues.add("Prediction");
            saliencyValues.add(IOUtils.roundedString(saliency.getOutput().getValue().asNumber(), decimalPlaces));
            confidences.add("");
            lineIDX++;
        }
        return IOUtils.generateTable(
                headers,
                headerPositions,
                lineSeparatorPositions,
                List.of(featureNames, featureValues, saliencyValues, confidences),
                List.of("", " | ", "")).getFirst();
    }
}
