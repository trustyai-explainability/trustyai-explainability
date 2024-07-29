package org.kie.trustyai.service.endpoints.explainers;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.connectors.kserve.v2.KServeConfig;
import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.dataframe.DataframeMetadata;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;
import org.kie.trustyai.service.validators.serviceRequests.LocalServiceURLValidator;

public abstract class ExplainerEndpoint {

    public static final String BIAS_IGNORE_PARAM = "bias-ignore";

    protected PredictionProvider getModel(ModelConfig modelConfig) throws IllegalArgumentException {
        return getModel(modelConfig, DataframeMetadata.DEFAULT_INPUT_TENSOR_NAME);
    }

    protected PredictionProvider getModel(ModelConfig modelConfig, String inputTensorName) throws IllegalArgumentException {
        return getModel(modelConfig, inputTensorName, null);
    }

    protected PredictionProvider getModel(ModelConfig modelConfig, String inputTensorName, String outputTensorName) throws IllegalArgumentException {
        final Map<String, String> map = new HashMap<>();
        map.put(BIAS_IGNORE_PARAM, "true");
        final String target = modelConfig.getTarget();
        if (!LocalServiceURLValidator.isValidUrl(target)) {
            throw new IllegalArgumentException("Invalid target URL: " + modelConfig.getTarget());
        }
        final KServeConfig kServeConfig = KServeConfig.create(
                target,
                modelConfig.getName(),
                modelConfig.getVersion(),
                KServeConfig.DEFAULT_CODEC,
                1);
        return KServeV2GRPCPredictionProvider.forTarget(kServeConfig, inputTensorName, null, map);
    }

}
