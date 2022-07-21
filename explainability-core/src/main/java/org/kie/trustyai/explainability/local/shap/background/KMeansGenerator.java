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

package org.kie.trustyai.explainability.local.shap.background;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

public class KMeansGenerator implements BackgroundGenerator {
    List<PredictionInput> seeds;

    /**
     * Generate a K-Means Background Selector
     *
     * @param seeds: All or a subset of available training {@link PredictionInput}s.
     */
    public KMeansGenerator(List<PredictionInput> seeds) {
        this.seeds = seeds;
    }

    /**
     * Generate $n K-Means background points
     *
     * @param n: The total number of background points to generate. This functionally sets the number of clusters
     *        within the k-means clustering, thus generating n clusters from the seeds passed to the generator. The
     *        centroids of these clusters are the generated background points.
     */
    public List<PredictionInput> generate(int n) {
        PredictionInput prototypePI = seeds.get(0);
        List<DoublePoint> datapoints = seeds.stream().map(pi -> new DoublePoint(pi.getFeatures().stream()
                .mapToDouble(f -> f.getValue().asNumber()).toArray()))
                .collect(Collectors.toList());
        KMeansPlusPlusClusterer<DoublePoint> clusterer = new KMeansPlusPlusClusterer<>(n);
        List<CentroidCluster<DoublePoint>> clusters = clusterer.cluster(datapoints);
        List<PredictionInput> background = new ArrayList<>();
        for (CentroidCluster c : clusters) {
            double[] center = c.getCenter().getPoint();
            List<Feature> newFeatures = new ArrayList<>();
            for (int i = 0; i < center.length; i++) {
                Feature f = prototypePI.getFeatures().get(i);
                if (f.getType() != Type.NUMBER) {
                    throw new IllegalArgumentException("KMeans Background Selection Can Only Be Called On Numeric Features");
                }
                newFeatures.add(new Feature(f.getName(), f.getType(), new Value(center[i])));
            }
            background.add(new PredictionInput(newFeatures));
        }
        return background;
    }
}
