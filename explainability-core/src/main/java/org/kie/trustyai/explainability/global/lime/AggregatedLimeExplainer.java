/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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
package org.kie.trustyai.explainability.global.lime;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.kie.trustyai.explainability.global.GlobalExplainer;
import org.kie.trustyai.explainability.local.lime.LimeExplainer;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.PredictionProviderMetadata;
import org.kie.trustyai.explainability.model.Saliency;
import org.kie.trustyai.explainability.model.SaliencyResults;
import org.kie.trustyai.explainability.utils.DataUtils;

/**
 * Global explainer aggregating LIME explanations over a number of inputs by reporting the mean feature importance for
 * each feature.
 */
public class AggregatedLimeExplainer implements GlobalExplainer<CompletableFuture<SaliencyResults>> {

    private final LimeExplainer limeExplainer;

    public AggregatedLimeExplainer() {
        this.limeExplainer = new LimeExplainer();
    }

    public AggregatedLimeExplainer(LimeExplainer limeExplainer) {
        this.limeExplainer = limeExplainer;
    }

    @Override
    public CompletableFuture<SaliencyResults> explainFromMetadata(PredictionProvider model, PredictionProviderMetadata metadata) {
        List<PredictionInput> inputs = metadata.getDataDistribution().sample(limeExplainer.getLimeConfig().getNoOfSamples()); // sample inputs from the data distribution

        return model.predictAsync(inputs) // execute the model on the inputs
                .thenApply(os -> DataUtils.getPredictions(inputs, os)) // generate predictions from inputs and outputs
                .thenCompose(ps -> explainFromPredictions(model, ps)); // explain predictions
    }

    @Override
    public CompletableFuture<SaliencyResults> explainFromPredictions(PredictionProvider model, Collection<Prediction> predictions) {
        return CompletableFuture.completedFuture(predictions)
                .thenApply(p -> p.stream().map(prediction -> limeExplainer.explainAsync(prediction, model)) // extract saliency for each input
                        .map(CompletableFuture::join) // aggregate all the saliencies
                        .reduce(new SaliencyResults(new HashMap<>(), SaliencyResults.SourceExplainer.AGGREGATED_LIME),
                                (m1, m2) -> new SaliencyResults(Saliency.merge(List.of(m1.getSaliencies(), m2.getSaliencies())), SaliencyResults.SourceExplainer.AGGREGATED_LIME))); // merge all the saliencies together
    }
}
