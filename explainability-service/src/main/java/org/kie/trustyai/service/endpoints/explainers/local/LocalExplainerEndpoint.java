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
package org.kie.trustyai.service.endpoints.explainers.local;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.explainability.BaseExplanationResponse;
import org.kie.trustyai.service.payloads.explainability.LocalExplanationRequest;

public abstract class LocalExplainerEndpoint extends ExplainerEndpoint {

    protected Response processRequest(LocalExplanationRequest request, DataSource dataSource, ServiceConfig serviceConfig) {
        try {
            String modelId = request.getModelId();
            String modelVersion = request.getModelVersion();
            PredictionProvider model = getModel(serviceConfig, modelId, modelVersion);

            Dataframe dataframe = dataSource.getDataframe(modelId);
            List<Prediction> predictions = dataframe.asPredictions();
            // TODO: check if we can fetch and use the prediction/payload id rather than an hash
            Predicate<Prediction> idFilter = prediction -> prediction.getInput().hashCode() == Integer.parseInt(request.getPredictionId());
            Prediction predictionToExplain = predictions.stream().filter(idFilter).findFirst().orElseThrow();
            List<PredictionInput> testDataDistribution = predictions.stream().filter(idFilter.negate()).map(Prediction::getInput)
                    .distinct().limit(serviceConfig.batchSize().orElse(100)).collect(Collectors.toList());

            predictionToExplain = prepare(predictionToExplain, request, testDataDistribution);

            BaseExplanationResponse entity = generateExplanation(model, predictionToExplain, testDataDistribution);
            return Response.ok(entity).build();
        } catch (Exception e) {
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    protected abstract BaseExplanationResponse generateExplanation(PredictionProvider model, Prediction predictionToExplain,
            List<PredictionInput> inputs);

    protected abstract Prediction prepare(Prediction prediction, LocalExplanationRequest request, List<PredictionInput> testData);

}
