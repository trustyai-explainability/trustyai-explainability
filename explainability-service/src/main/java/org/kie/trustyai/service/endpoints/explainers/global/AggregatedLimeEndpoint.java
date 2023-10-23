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
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.global.lime.AggregatedLimeExplainer;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.payloads.explainers.BaseExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.GlobalExplanationRequest;
import org.kie.trustyai.service.payloads.explainers.SaliencyExplanationResponse;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Aggregated LIME Explainer Endpoint")
@Path("/explainers/global/lime")
public class AggregatedLimeEndpoint extends GlobalExplainerEndpoint {

    private static final Logger LOG = Logger.getLogger(AggregatedLimeEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response explain(GlobalExplanationRequest request) {
        return processRequest(request, dataSource.get());
    }

    @Override
    protected BaseExplanationResponse generateExplanation(PredictionProvider model, List<Prediction> inputs) {
        AggregatedLimeExplainer limeExplainer = new AggregatedLimeExplainer();
        try {
            return SaliencyExplanationResponse.fromSaliencyResults(limeExplainer.explainFromPredictions(model, inputs).get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to provide global explanation", e);
            return SaliencyExplanationResponse.empty();
        }
    }
}
