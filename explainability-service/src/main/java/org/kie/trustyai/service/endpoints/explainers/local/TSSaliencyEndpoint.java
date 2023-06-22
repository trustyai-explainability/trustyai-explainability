package org.kie.trustyai.service.endpoints.explainers.local;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.local.tssaliency.TSSaliencyExplainer;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.utils.models.TSSaliencyModel;
import org.kie.trustyai.service.payloads.requests.TSSaliencyRequest;

@Tag(name = "TSSaliency Explainer Endpoint")
@Path("/explainers/local/tssaliency")
public class TSSaliencyEndpoint {

    private static final Logger LOG = Logger.getLogger(TSSaliencyEndpoint.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response explain(TSSaliencyRequest request) {
        final PredictionProvider testModel = new TSSaliencyModel();
        // Convert the request data to Prediction Inputs
        final Map<String, List<Double>> data = request.getData();

        final List<PredictionInput> inputs = data.values().stream().map(values -> {
            List<Feature> features = new ArrayList<>();
            for (int n = 0; n < values.size(); n++) {
                double value = values.get(n);
                features.add(FeatureFactory.newVectorFeature("element" + n, value));
            }
            return features;
        }).map(PredictionInput::new).collect(Collectors.toList());

        final CompletableFuture<List<PredictionOutput>> result = testModel.predictAsync(inputs);
        List<PredictionOutput> results;
        try {
            results = result.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
            //            return Response.serverError().status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
        PredictionInput predictionInput = inputs.get(0);

        PredictionOutput predictionOutput = results.get(0);

        UUID uuid = UUID.randomUUID();

        Prediction prediction = new SimplePrediction(predictionInput, predictionOutput, uuid);
        final TSSaliencyExplainer explainer = new TSSaliencyExplainer(new double[0], 50, 1000, 0);
        final SaliencyResults explanation;
        try {
            explanation = explainer.explainAsync(prediction, testModel).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Saliency results: " + explanation);
        Map<String, Saliency> saliencyMap = explanation.getSaliencies();
        Saliency saliency = saliencyMap.get("result");
        List<FeatureImportance> featureImportances = saliency.getPerFeatureImportance();
        FeatureImportance featureImportance = featureImportances.get(0);

        double[][] scoreResult = featureImportance.getScoreMatrix();

        for (int t = 0; t < 500; t++) {
            for (int f = 0; f < 1; f++) {
                double score = scoreResult[t][f];
                System.out.println(score);
            }
        }

        return Response.serverError().status(Response.Status.OK).entity(saliency).build();
    }

}
