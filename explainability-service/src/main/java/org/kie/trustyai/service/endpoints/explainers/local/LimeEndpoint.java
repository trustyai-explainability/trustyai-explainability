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
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.local.lime.LimeConfig;
import org.kie.trustyai.explainability.local.lime.LimeExplainer;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionInputsDataDistribution;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.explainers.BaseExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.SaliencyExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.lime.LimeExplainerConfig;
import org.kie.trustyai.service.payloads.explainers.lime.LimeExplanationRequest;

import io.quarkus.resteasy.reactive.server.EndpointDisabled;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Explainers: Local", description = "Local explainers provide explanation of model behavior over a single prediction.")
@EndpointDisabled(name = "endpoints.explainers.local.lime", stringValue = "disable")
@Path("/explainers/local/lime")
public class LimeEndpoint extends ExplainerEndpoint {

    private static final Logger LOG = Logger.getLogger(LimeEndpoint.class);
    @Inject
    Instance<DataSource> dataSource;

    @Inject
    ServiceConfig serviceConfig;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Compute a LIME explanation.", description = "Generate a LIME explanation for a given model and inference id")
    public Response explain(LimeExplanationRequest request) {
        final String inferenceId = request.getPredictionId();
        final String modelId = request.getConfig().getModelConfig().getName();
        try {
            final Dataframe dataframe = dataSource.get().getDataframeFilteredByIds(modelId, Set.of(inferenceId));
            final PredictionProvider model = getModel(request.getConfig().getModelConfig(),
                    dataframe.getInputTensorName());

            Prediction predictionToExplain;
            final List<Prediction> predictions = dataSource.get().getDataframe(modelId)
                    .filterRowsById(inferenceId).asPredictions();
            if (predictions.isEmpty()) {
                return Response.status(Response.Status.NOT_FOUND).entity("No prediction found with id="
                        + inferenceId).build();
            } else if (predictions.size() == 1) {
                predictionToExplain = predictions.get(0);
                final int backgroundSize = serviceConfig.batchSize().orElse(100);
                final Dataframe organicDataframe = dataSource.get().getOrganicDataframe(modelId);
                final List<PredictionInput> testDataDistribution = organicDataframe.filterRowsById(inferenceId, true,
                        backgroundSize).asPredictionInputs();
                final BaseExplanationResponse entity = generateExplanation(model, predictionToExplain,
                        testDataDistribution, request);
                return Response.ok(entity).build();
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Found " + predictions.size()
                        + " predictions with id=" + request.getPredictionId()).build();
            }
        } catch (TimeoutException e) {
            return Response.serverError().status(Response.Status.REQUEST_TIMEOUT).entity("The explanation request has timed out").build();
        } catch (Exception e) {
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    public BaseExplanationResponse generateExplanation(PredictionProvider model, Prediction predictionToExplain,
            List<PredictionInput> inputs, LimeExplanationRequest request) throws ExecutionException, InterruptedException, TimeoutException {
        final LimeExplainerConfig requestConfig = request.getConfig().getExplainerConfig();
        final LimeConfig config = new LimeConfig()
                .withSamples(requestConfig.getnSamples())
                .withSeparableDatasetRatio(requestConfig.getSeparableDatasetRation())
                .withRetries(requestConfig.getRetries())
                .withAdaptiveVariance(requestConfig.isAdaptiveVariance())
                .withPenalizeBalanceSparse(requestConfig.isPenalizeBalanceSparse())
                .withProximityFilter(requestConfig.isProximityFilter())
                .withProximityThreshold(requestConfig.getProximityThreshold())
                .withProximityKernelWidth(requestConfig.getProximityKernelWidth())
                .withProximityThreshold(requestConfig.getEncodingClusterThreshold())
                .withNormalizeWeights(requestConfig.isNormalizeWeights())
                .withHighScoreFeatureZones(requestConfig.isHighScoreFeatureZones())
                .withFeatureSelection(requestConfig.isFeatureSelection())
                .withNoOfFeatures(requestConfig.getnFeatures())
                .withTrackCounterfactuals(requestConfig.isTrackCounterfactuals())
                .withUseWLRLinearModel(requestConfig.isUseWLRModel())
                .withFilterInterpretable(requestConfig.isFilterInterpretable())
                .withDataDistribution(new PredictionInputsDataDistribution(inputs));

        final LimeExplainer limeExplainer = new LimeExplainer(config); // TODO: switch to RecordingLimeExplainer?
        return SaliencyExplanationResponse
                .fromSaliencyResults(limeExplainer.explainAsync(predictionToExplain, model).get(requestConfig.getTimeout(), TimeUnit.SECONDS));
    }

}
