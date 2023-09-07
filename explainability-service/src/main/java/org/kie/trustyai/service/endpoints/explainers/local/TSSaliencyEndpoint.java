package org.kie.trustyai.service.endpoints.explainers.local;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.CodecParameter;
import org.kie.trustyai.connectors.kserve.v2.KServeConfig;
import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.explainability.local.tssaliency.TSSaliencyExplainer;
import org.kie.trustyai.explainability.local.tssaliency.TSSaliencyModelWrapper;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.utils.TimeseriesUtils;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.endpoints.explainers.ExplainerEndpoint;
import org.kie.trustyai.service.payloads.explainers.ModelConfig;
import org.kie.trustyai.service.payloads.explainers.tssaliency.TSSaliencyParameters;
import org.kie.trustyai.service.payloads.explainers.tssaliency.TSSaliencyRequest;

@Tag(name = "TSSaliency Explainer Endpoint")
@Path("/explainers/local/tssaliency")
public class TSSaliencyEndpoint extends ExplainerEndpoint {

    private static final Logger LOG = Logger.getLogger(TSSaliencyEndpoint.class);
    private static double[] DEFAULT_BASE_VALUE = new double[0];
    @Inject
    ServiceConfig serviceConfig;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response explain(TSSaliencyRequest request) {

        // Instantiate the model from the request
        final ModelConfig modelConfig = request.getModelConfig();
        final KServeConfig kServeConfig = KServeConfig.create(modelConfig.getTarget(), modelConfig.getName(), modelConfig.getVersion(), CodecParameter.NP);
        final Map<String, String> optionalParameters = new HashMap<>();
        optionalParameters.put(BIAS_IGNORE_PARAM, "true");
        final PredictionProvider originalModel = KServeV2GRPCPredictionProvider.forTarget(kServeConfig, new ArrayList<>(request.getData().keySet()), optionalParameters);
        final PredictionProvider wrappedModel = new TSSaliencyModelWrapper(originalModel);

        // Convert the request data to Prediction Inputs
        final Map<String, List<Double>> data = request.getData();

        final int size = data.values().iterator().next().size();
        final List<List<Feature>> transposed = new ArrayList<>(size);

        // Initialize list of lists
        for (int i = 0; i < size; i++) {
            transposed.add(new ArrayList<>());
        }

        for (List<Double> list : data.values()) {
            for (int i = 0; i < size; i++) {
                transposed.get(i).add(FeatureFactory.newNumericalFeature("t-" + i, list.get(i)));
            }
        }

        List<PredictionInput> inputs = new ArrayList<>(size);
        for (List<Feature> list : transposed) {
            inputs.add(new PredictionInput(list));
        }

        final CompletableFuture<List<PredictionOutput>> result = originalModel.predictAsync(inputs);

        List<PredictionOutput> results;
        try {
            results = result.get(20L, TimeUnit.SECONDS);
            for (PredictionOutput predictionOutput : results) {
                LOG.info("Prediction output: " + predictionOutput.getOutputs());
            }
        } catch (InterruptedException | ExecutionException e) {
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        } catch (TimeoutException e) {
            return Response.serverError().status(Response.Status.REQUEST_TIMEOUT).entity(e.getMessage()).build();
        }

        final List<Prediction> predictions = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            predictions.add(new SimplePrediction(TimeseriesUtils.featureListTofeatureVector(inputs.get(i)), results.get(i)));
        }

        final int randomSeed = new Random().nextInt();

        final TSSaliencyParameters parameters = request.getParameters();
        final TSSaliencyExplainer explainer = new TSSaliencyExplainer(DEFAULT_BASE_VALUE,
                parameters.getnSamples(),
                parameters.getnSteps(),
                randomSeed,
                parameters.getSigma(),
                parameters.getMu());
        final SaliencyResults explanation;
        try {
            explanation = explainer.explainAsync(predictions, wrappedModel).get();
        } catch (InterruptedException | ExecutionException e) {
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        LOG.debug("Saliency results: " + explanation);
        final Map<String, Saliency> saliencyMap = explanation.getSaliencies();
        return Response.serverError().status(Response.Status.OK).entity(saliencyMap).build();
    }

}
