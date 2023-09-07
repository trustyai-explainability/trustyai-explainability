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
import java.util.concurrent.ExecutionException;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.local.shap.ShapConfig;
import org.kie.trustyai.explainability.local.shap.ShapKernelExplainer;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.payloads.explainers.BaseExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.LocalExplanationRequest;
import org.kie.trustyai.service.payloads.explainers.SaliencyExplanationResponse;

@Tag(name = "SHAP Explainer Endpoint")
@Path("/explainers/local/shap")
public class SHAPEndpoint extends LocalExplainerEndpoint {

    private static final Logger LOG = Logger.getLogger(SHAPEndpoint.class);

    @Inject
    Instance<DataSource> dataSource;

    @Inject
    ServiceConfig serviceConfig;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response explain(LocalExplanationRequest request) {
        return processRequest(request, dataSource.get(), serviceConfig);
    }

    @Override
    public BaseExplanationResponse generateExplanation(PredictionProvider model, Prediction predictionToExplain, List<PredictionInput> inputs) {
        ShapKernelExplainer shapKernelExplainer = new ShapKernelExplainer(ShapConfig.builder().withBackground(inputs).build());
        try {
            return SaliencyExplanationResponse.fromSaliencyResults(shapKernelExplainer.explainAsync(predictionToExplain, model).get());
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to explain {} ", predictionToExplain, e);
            return SaliencyExplanationResponse.empty();
        }
    }

    @Override
    protected Prediction prepare(Prediction prediction, LocalExplanationRequest request, List<PredictionInput> testData) {
        return prediction;
    }
}
