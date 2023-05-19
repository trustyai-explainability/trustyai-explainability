package org.kie.trustyai.service.endpoints.explainers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.payloads.BaseExplanationRequest;
import org.kie.trustyai.service.payloads.BaseExplanationResponse;

public abstract class ExplainerEndpoint {

    protected static final String BIAS_IGNORE_PARAM = "bias-ignore";

    protected Response processRequest(BaseExplanationRequest request, DataSource dataSource, ServiceConfig serviceConfig) {
        try {
            String modelId = request.getModelId();
            PredictionProvider model = getModel(serviceConfig, modelId);

            Dataframe dataframe = dataSource.getDataframe(modelId);
            List<Prediction> predictions = dataframe.asPredictions();
            // TODO: check if we can fetch and use the prediction/payload id rather than an hash
            Predicate<Prediction> idFilter = prediction -> prediction.getInput().hashCode() == Integer.parseInt(request.getPredictionId());
            Prediction predictionToExplain = predictions.stream().filter(idFilter).findFirst().orElseThrow();
            List<PredictionInput> testDataDistribution = predictions.stream().filter(idFilter.negate()).map(Prediction::getInput)
                    .distinct().limit(serviceConfig.batchSize().orElse(100)).collect(Collectors.toList());

            predictionToExplain = prepare(predictionToExplain, request, testDataDistribution);

            BaseExplanationResponse entity = generateExplanation(model, predictionToExplain, testDataDistribution);
            entity.setId(request.getId());
            return Response.ok(entity).build();
        } catch (Exception e) {
            return Response.serverError().status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    protected abstract BaseExplanationResponse generateExplanation(PredictionProvider model, Prediction predictionToExplain,
            List<PredictionInput> inputs);

    protected abstract Prediction prepare(Prediction prediction, BaseExplanationRequest request, List<PredictionInput> testData);

    protected PredictionProvider getModel(ServiceConfig serviceConfig, String modelId) throws Exception {
        String target = serviceConfig.kserveTarget().orElseThrow(() -> new Exception("kserve/model-mesh service endpoint not specified"));
        Map<String, String> map = new HashMap<>();
        map.put(BIAS_IGNORE_PARAM, "true");
        return KServeV2GRPCPredictionProvider.forTarget(target, modelId, map);
    }
}
