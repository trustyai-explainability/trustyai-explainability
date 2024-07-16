package org.kie.trustyai.service.endpoints.explainers;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.connectors.kserve.v2.KServeConfig;
import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.dataframe.DataframeMetadata;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

public abstract class ExplainerEndpoint {

    public static final String BIAS_IGNORE_PARAM = "bias-ignore";

    protected PredictionProvider getModel(ModelConfig modelConfig) {
        return getModel(modelConfig, DataframeMetadata.DEFAULT_INPUT_TENSOR_NAME);
    }

    protected PredictionProvider getModel(ModelConfig modelConfig, String inputTensorName) {
        return getModel(modelConfig, inputTensorName, null);
    }

    protected PredictionProvider getModel(ModelConfig modelConfig, String inputTensorName, String outputTensorName) {
        final Map<String, String> map = new HashMap<>();
        map.put(BIAS_IGNORE_PARAM, "true");
        final KServeConfig kServeConfig = KServeConfig.create(modelConfig.getTarget(), modelConfig.getName(), modelConfig.getVersion());
        return KServeV2GRPCPredictionProvider.forTarget(kServeConfig, inputTensorName, null, map);
    }

}
