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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.local.shap.ShapConfig;
import org.kie.trustyai.explainability.local.shap.ShapKernelExplainer;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.SaliencyResults;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.explainers.BaseExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.SaliencyExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.shap.SHAPExplainerConfig;
import org.kie.trustyai.service.payloads.explainers.shap.SHAPExplanationRequest;

import io.quarkus.resteasy.reactive.server.EndpointDisabled;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Local explainers")
@EndpointDisabled(name = "endpoints.explainers.local", stringValue = "disable")
@Path("/explainers/local/shap")
public class SHAPEndpoint extends ExplainerEndpoint {

    private static final Logger LOG = Logger.getLogger(SHAPEndpoint.class);

    @Inject
    Instance<DataSource> dataSource;

    @Inject
    ServiceConfig serviceConfig;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Generate a SHAP explanation", description = "Generate a SHAP explanation for a given model and inference id")
    public Response explain(SHAPExplanationRequest request) {
        final String inferenceId = request.getPredictionId();
        try {
            final String modelId = request.getConfig().getModelConfig().getName();
            final Dataframe dataframe = dataSource.get().getDataframe(modelId);
            final PredictionProvider model = getModel(request.getConfig().getModelConfig(), dataframe.getInputTensorName(),
                    dataframe.getOutputTensorName());

            final List<Prediction> predictions = dataframe.filterRowsById(inferenceId).asPredictions();
            if (predictions.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).entity("No prediction found with id="
                        + inferenceId).build();
            } else if (predictions.size() == 1) {
                final Prediction predictionToExplain = predictions.get(0);
                final int backgroundSize = serviceConfig.batchSize().orElse(100);
                final List<PredictionInput> testDataDistribution = dataframe.filterRowsById(inferenceId, true,
                        backgroundSize).asPredictionInputs();
                final BaseExplanationResponse entity = generateExplanation(model, predictionToExplain, testDataDistribution, request);
                return Response.ok(entity).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Found " + predictions.size()
                        + " predictions with id=" + inferenceId).build();
            }
        } catch (Exception e) {
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    public BaseExplanationResponse generateExplanation(PredictionProvider model, Prediction predictionToExplain, List<PredictionInput> inputs, SHAPExplanationRequest request) {
        final SHAPExplainerConfig requestConfig = request.getConfig().getExplainerConfig();
        final ShapConfig config = ShapConfig.builder()
                .withNSamples(requestConfig.getnSamples())
                .withLink(requestConfig.getLinkType())
                .withRegularizer(requestConfig.getRegularizer())
                .withConfidence(requestConfig.getConfidence())
                .withTrackCounterfactuals(requestConfig.isTrackCounterfactuals())
                .withBackground(inputs)
                .build();
        final ShapKernelExplainer shapKernelExplainer = new ShapKernelExplainer(config);
        try {
            final SaliencyResults explanation = shapKernelExplainer.explainAsync(predictionToExplain, model).get();
            return SaliencyExplanationResponse.fromSaliencyResults(explanation);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to explain {} ", predictionToExplain, e);
            return SaliencyExplanationResponse.empty();
        }
    }

}
