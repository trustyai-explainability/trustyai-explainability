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

import javax.ws.rs.core.Response;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.explainers.BaseExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.LocalExplanationRequest;

public abstract class LocalExplainerEndpoint extends ExplainerEndpoint {

    protected Response processRequest(LocalExplanationRequest request, DataSource dataSource, ServiceConfig serviceConfig) {
        try {
            PredictionProvider model = getModel(request.getModelConfig());

            Dataframe dataframe = dataSource.getDataframe(request.getModelConfig().getName());
            Prediction predictionToExplain;
            List<Prediction> predictions = dataframe.filterRowsById(request.getPredictionId()).asPredictions();
            if (predictions.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).entity("No prediction found with id="
                        + request.getPredictionId()).build();
            } else if (predictions.size() == 1) {
                predictionToExplain = predictions.get(0);
                List<PredictionInput> testDataDistribution = dataframe.filterRowsById(request.getPredictionId(), true,
                        serviceConfig.batchSize().orElse(100)).asPredictionInputs();
                predictionToExplain = prepare(predictionToExplain, request, testDataDistribution);
                BaseExplanationResponse entity = generateExplanation(model, predictionToExplain, testDataDistribution);
                return Response.ok(entity).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Found " + predictions.size()
                        + " predictions with id=" + request.getPredictionId()).build();
            }
        } catch (Exception e) {
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    protected abstract BaseExplanationResponse generateExplanation(PredictionProvider model, Prediction predictionToExplain,
            List<PredictionInput> inputs);

    protected abstract Prediction prepare(Prediction prediction, LocalExplanationRequest request, List<PredictionInput> testData);

}
