package org.kie.trustyai.service.endpoints.explainers.local;

import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.local.tssaliency.TSSaliencyExplainer;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.SaliencyResults;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;
import org.kie.trustyai.service.payloads.explainers.tssaliency.TSSaliencyExplainerConfig;
import org.kie.trustyai.service.payloads.explainers.tssaliency.TSSaliencyExplanationRequest;

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
@EndpointDisabled(name = "endpoints.explainers.local.tssaliency", stringValue = "disable")
@Path("/explainers/local/tssaliency")
public class TSSaliencyEndpoint extends ExplainerEndpoint {

    @Inject
    Instance<DataSource> dataSource;
    @Inject
    SecureRandom secureRandom;

    @POST
    @Operation(summary = "Compute a TSSaliency explanation.")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response explain(TSSaliencyExplanationRequest request) {

        try {
            final ModelConfig modelConfig = request.getConfig().getModelConfig();

            // Get the inference ids
            final List<String> inferenceIds = request.getPredictionIds();

            final String modelId = modelConfig.getName();
            final Dataframe dataframe = dataSource.get().getOrganicDataframe(modelId);

            final List<Prediction> predictions = dataSource.get()
                    .getDataframe(modelId)
                    .filterByInternalColumnValue(Dataframe.InternalColumn.ID,
                            value -> inferenceIds.contains(value.getUnderlyingObject().toString()))
                    .asPredictions();

            if (predictions.isEmpty()) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("No predictions found for the provided inference ids").build();

            } else if (predictions.size() != inferenceIds.size()) {
                return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Not all provided inference ids found").build();
            } else {
                final PredictionProvider model = getModel(modelConfig, dataframe.getInputTensorName());
                final TSSaliencyExplainerConfig parameters = request.getConfig().getExplainerConfig();
                final int nFeatures = predictions.size();
                final double[] baseValues;
                final double[] requestBaseValues = request.getConfig().getExplainerConfig().getBaseValues();

                if (requestBaseValues.length == 0) {
                    baseValues = new double[nFeatures];
                } else {
                    baseValues = requestBaseValues;
                    if (requestBaseValues.length != nFeatures) {
                        throw new IllegalArgumentException("Base values has " + baseValues.length
                                + " elements. Must have the same number as features (" + nFeatures + ")");
                    }

                }
                final TSSaliencyExplainer explainer = new TSSaliencyExplainer(
                        baseValues,
                        nFeatures + parameters.getnSamples(),
                        nFeatures + parameters.getnAlpha(),
                        secureRandom.nextInt(),
                        parameters.getSigma(),
                        parameters.getMu(),
                        true);
                final SaliencyResults explanations = explainer.explainAsync(predictions, model).get(request.getConfig().getExplainerConfig().getTimeout(), TimeUnit.SECONDS);
                return Response.ok(explanations).build();
            }
        } catch (TimeoutException e) {
            return Response.serverError().status(Response.Status.REQUEST_TIMEOUT).entity("The explanation request has timed out").build();
        } catch (Exception e) {
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

}
