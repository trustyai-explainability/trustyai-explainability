/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates.
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
package org.kie.trustyai.service.payloads;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.PartialDependenceGraph;
import org.kie.trustyai.explainability.model.Value;

public class PartialDependencePlotExplanationResponse extends BaseExplanationResponse {

    private List<FeaturePartialDependence> perFeaturePartialDependence;

    public static BaseExplanationResponse empty() {
        return new PartialDependencePlotExplanationResponse();
    }

    public List<FeaturePartialDependence> getPerFeaturePartialDependence() {
        return perFeaturePartialDependence;
    }

    public void setPerFeaturePartialDependence(List<FeaturePartialDependence> perFeaturePartialDependence) {
        this.perFeaturePartialDependence = perFeaturePartialDependence;
    }

    public static BaseExplanationResponse fromGraphs(List<PartialDependenceGraph> pdgs) {
        List<FeaturePartialDependence> perFeaturePartialDependence = new ArrayList<>();
        for (PartialDependenceGraph pdg : pdgs) {
            FeaturePartialDependence featurePartialDependence = new FeaturePartialDependence();
            featurePartialDependence.setFeatureName(pdg.getFeature().getName());
            featurePartialDependence.setX(pdg.getX().stream().map(Value::asString).collect(Collectors.toList()));
            featurePartialDependence.setY(pdg.getY().stream().map(Value::asString).collect(Collectors.toList()));
            perFeaturePartialDependence.add(featurePartialDependence);
        }
        PartialDependencePlotExplanationResponse response = new PartialDependencePlotExplanationResponse();
        response.setPerFeaturePartialDependence(perFeaturePartialDependence);
        return response;
    }

    public static class FeaturePartialDependence {
        private String featureName;
        private List<String> x;
        private List<String> y;

        public String getFeatureName() {
            return featureName;
        }

        public void setFeatureName(String featureName) {
            this.featureName = featureName;
        }

        public List<String> getX() {
            return x;
        }

        public void setX(List<String> x) {
            this.x = x;
        }

        public List<String> getY() {
            return y;
        }

        public void setY(List<String> y) {
            this.y = y;
        }
    }

}
