package org.kie.trustyai.service.endpoints.explainers;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.service.config.ServiceConfig;

public abstract class ExplainerEndpoint {

    public static final String BIAS_IGNORE_PARAM = "bias-ignore";

    protected PredictionProvider getModel(ServiceConfig serviceConfig, String modelId) throws Exception {
        String target = serviceConfig.kserveTarget().orElseThrow(() -> new Exception("kserve/model-mesh service endpoint not specified"));
        Map<String, String> map = new HashMap<>();
        map.put(BIAS_IGNORE_PARAM, "true");
        return KServeV2GRPCPredictionProvider.forTarget(target, modelId, map);
    }
}
