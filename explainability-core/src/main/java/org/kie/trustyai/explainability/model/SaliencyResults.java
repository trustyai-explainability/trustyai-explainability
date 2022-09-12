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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.kie.trustyai.explainability.utils.IOUtils;

public class SaliencyResults {
    private final String explainerName;
    private final Map<String, Saliency> saliencies;

    public static SaliencyResults newAggregatedLimeResults(Map<String, Saliency> saliencies) {
        return new SaliencyResults("Aggregated LIME", saliencies);
    }

    public static SaliencyResults newLimeResults(Map<String, Saliency> saliencies) {
        return new SaliencyResults("LIME", saliencies);
    }

    public static SaliencyResults newShapResults(Map<String, Saliency> saliencies) {
        return new SaliencyResults("SHAP", saliencies);
    }

    private SaliencyResults(String explainerName, Map<String, Saliency> saliencies) {
        this.explainerName = explainerName;
        this.saliencies = saliencies;
    }

    public Map<String, Saliency> getSaliencies() {
        return saliencies;
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
     * Represent the SaliencyResults as a string
     *
     * @return ShapResult string
     */
    public String asTable() {
        return asTable(3);
    }

    /**
     * Represent the SaliencyResults as a string
     *
     * @param decimalPlaces The decimal places to round all numeric values in the table to
     *
     * @return ShapResult string
     */
    public String asTable(int decimalPlaces) {
        List<String> featureNames = new ArrayList<>();
        List<String> featureValues = new ArrayList<>();
        List<String> saliencyValues = new ArrayList<>();
        List<String> confidences = new ArrayList<>();

        List<String> headers = new ArrayList<>();
        List<Integer> headerPositions = new ArrayList<>();
        List<Integer> lineSeparatorPositions = new ArrayList<>();
        int lineIDX = 0;

        for (Map.Entry<String, Saliency> entry : saliencies.entrySet()) {
            Saliency saliency = entry.getValue();
            List<FeatureImportance> pfis = saliency.getPerFeatureImportance();
            headers.add(entry.getKey() + " " + explainerName + " Saliencies");
            headerPositions.add(lineIDX);

            featureNames.add("Feature");
            featureValues.add("Value");
            saliencyValues.add(explainerName + " Saliency");
            confidences.add(" | Confidence");
            lineIDX++;

            for (int i = 0; i < pfis.size(); i++) {
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
