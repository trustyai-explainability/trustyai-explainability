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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.local.counterfactual.CounterfactualExplainer;
import org.kie.trustyai.explainability.local.counterfactual.CounterfactualResult;
import org.kie.trustyai.explainability.model.CounterfactualPrediction;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureDistribution;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionInputsDataDistribution;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.domain.FeatureDomain;
import org.kie.trustyai.explainability.utils.DataUtils;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.explainers.BaseExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.CounterfactualExplanationRequest;
import org.kie.trustyai.service.payloads.explainers.CounterfactualExplanationResponse;
import org.kie.trustyai.service.payloads.explainers.LocalExplanationRequest;

import io.quarkus.resteasy.reactive.server.EndpointDisabled;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Tag(name = "Counterfactual Explainer Endpoint")
@EndpointDisabled(name = "endpoints.explainers.local", stringValue = "disable")
@Path("/explainers/local/cf")
public class CounterfactualEndpoint extends LocalExplainerEndpoint {

    private static final Logger LOG = Logger.getLogger(CounterfactualEndpoint.class);

    @Inject
    Instance<DataSource> dataSource;

    @Inject
    ServiceConfig serviceConfig;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response explain(CounterfactualExplanationRequest request) throws DataframeCreateException {
        return processRequest(request, dataSource.get(), serviceConfig);
    }

    @Override
    public BaseExplanationResponse generateExplanation(PredictionProvider model, Prediction predictionToExplain, List<PredictionInput> inputs) {
        CounterfactualExplainer counterfactualExplainer = new CounterfactualExplainer();
        try {
            CounterfactualResult counterfactualResult = counterfactualExplainer.explainAsync(predictionToExplain, model).get();
            return CounterfactualExplanationResponse.from(counterfactualResult);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to explain {} ", predictionToExplain, e);
            return CounterfactualExplanationResponse.empty();
        }
    }

    @Override
    protected Prediction prepare(Prediction prediction, LocalExplanationRequest request, List<PredictionInput> testData) {
        PredictionInput input = prediction.getInput();

        List<Feature> cfInputFeatures = new ArrayList<>();
        PredictionInputsDataDistribution dataDistribution = new PredictionInputsDataDistribution(testData);
        List<FeatureDistribution> featureDistributions = dataDistribution.asFeatureDistributions();
        for (FeatureDistribution featureDistribution : featureDistributions) {
            String name = featureDistribution.getFeature().getName();
            Feature feature = input.getFeatureByName(name).orElse(null);
            if (feature != null) {
                FeatureDomain featureDomain = DataUtils.toFeatureDomain(featureDistribution);
                Feature cfFeature = new Feature(name, feature.getType(), feature.getValue(), feature.isConstrained(),
                        featureDomain);
                cfInputFeatures.add(cfFeature);
            }
        }

        List<Output> goals = new ArrayList<>();
        CounterfactualExplanationRequest counterfactualExplanationRequest = (CounterfactualExplanationRequest) request;
        Map<String, String> targetGoals = counterfactualExplanationRequest.getGoals();
        for (Output output : prediction.getOutput().getOutputs()) {
            String newGoalValue = targetGoals.get(output.getName());
            if (newGoalValue != null) {
                goals.add(new Output(output.getName(), output.getType(), new Value(newGoalValue), output.getScore()));
            }
        }

        return new CounterfactualPrediction(new PredictionInput(cfInputFeatures), new PredictionOutput(goals),
                dataDistribution, prediction.getExecutionId(), 300L);
    }
}
