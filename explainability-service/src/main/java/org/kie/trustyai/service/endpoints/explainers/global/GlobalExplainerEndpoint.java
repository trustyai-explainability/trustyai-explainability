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
package org.kie.trustyai.service.endpoints.explainers.global;

import java.util.List;

import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.explainers.BaseExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.GlobalExplanationRequest;

import jakarta.ws.rs.core.Response;

public abstract class GlobalExplainerEndpoint extends ExplainerEndpoint {

    protected Response processRequest(GlobalExplanationRequest request, DataSource dataSource) {
        try {
            PredictionProvider model = getModel(request.getModelConfig());

            Dataframe dataframe = dataSource.getDataframe(request.getModelConfig().getName());
            List<Prediction> predictions = dataframe.asPredictions();

            BaseExplanationResponse entity = generateExplanation(model, predictions);
            return Response.ok(entity).build();
        } catch (Exception e) {
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    protected abstract BaseExplanationResponse generateExplanation(PredictionProvider model, List<Prediction> inputs);

}
