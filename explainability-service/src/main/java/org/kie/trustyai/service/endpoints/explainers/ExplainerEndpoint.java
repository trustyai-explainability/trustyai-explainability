package org.kie.trustyai.service.endpoints.explainers;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.KServeConfig;
import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.connectors.utils.TLSCredentials;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.dataframe.DataframeMetadata;
import org.kie.trustyai.service.config.tls.SSLConfig;
import org.kie.trustyai.service.payloads.explainers.config.ModelConfig;

import jakarta.inject.Inject;

public abstract class ExplainerEndpoint {

    private static final Logger LOG = Logger.getLogger(ExplainerEndpoint.class);

    @Inject
    @ConfigProperty(name = "explainers.target")
    String target;

    @Inject
    SSLConfig sslConfig;

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
        LOG.info("Using model endpoint " + target);
        final KServeConfig kServeConfig = KServeConfig.create(
                target,
                modelConfig.getName(),
                modelConfig.getVersion(),
                KServeConfig.DEFAULT_CODEC,
                1);
        final TLSCredentials tlsCredentials = new TLSCredentials(
                new File(sslConfig.getCertificateFile().get(0)),
                new File(sslConfig.getKeyFile().get(0)),
                Optional.of(new File(sslConfig.getCertificateFile().get(0))));
        return KServeV2GRPCPredictionProvider.forTarget(
                kServeConfig,
                inputTensorName,
                null,
                map,
                Optional.of(tlsCredentials));
    }

}
