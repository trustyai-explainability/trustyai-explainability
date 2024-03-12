package org.kie.trustyai.service.endpoints.explainers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.connectors.kserve.v2.KServeConfig;
import org.kie.trustyai.connectors.kserve.v2.KServeV2GRPCPredictionProvider;
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.explainability.utils.models.TestModels;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrpcMockServerTest {

    private GrpcMockServer mockServer;

    @BeforeEach
    void setup() throws IOException {
        mockServer = new GrpcMockServer(TestModels.getFeatureSkipModel(1));
        mockServer.start();
    }

    @AfterEach
    void tearDown() {
        mockServer.stop();
    }

    @Test
    @DisplayName("Test with default tensor name")
    void testOutputDefaultTensorName() throws ExecutionException, InterruptedException {
        KServeConfig config = KServeConfig.create("localhost:" + mockServer.getPort(), "test", "1");
        PredictionProvider model = KServeV2GRPCPredictionProvider.forTarget(config);
        List<Feature> features = List.of(
                FeatureFactory.newNumericalFeature("d", 1.0),
                FeatureFactory.newNumericalFeature("e", 2.0),
                FeatureFactory.newNumericalFeature("f", 3.0));
        List<PredictionOutput> predictionOutputs = model.predictAsync(List.of(new PredictionInput(features))).get();

        assertEquals(1, predictionOutputs.size());
        final List<Output> outputs = predictionOutputs.get(0).getOutputs();
        assertEquals(2, outputs.size());
        assertEquals(1.0, outputs.get(0).getValue().asNumber());
        assertEquals(3.0, outputs.get(1).getValue().asNumber());

        assertEquals("output-0", outputs.get(0).getName());
        assertEquals("output-1", outputs.get(1).getName());

    }

    @Test
    @DisplayName("Test with custom tensor name")
    void testOutputCustomTensorName() throws ExecutionException, InterruptedException {
        KServeConfig config = KServeConfig.create("localhost:" + mockServer.getPort(), "test", "1");
        final List<String> customOutputNames = List.of("custom-output-a", "custom-output-b");
        PredictionProvider model = KServeV2GRPCPredictionProvider.forTarget(config, "test-input", customOutputNames, null);
        List<Feature> features = List.of(
                FeatureFactory.newNumericalFeature("d", 1.0),
                FeatureFactory.newNumericalFeature("e", 2.0),
                FeatureFactory.newNumericalFeature("f", 3.0));
        List<PredictionOutput> predictionOutputs = model.predictAsync(List.of(new PredictionInput(features))).get();

        assertEquals(1, predictionOutputs.size());
        final List<Output> outputs = predictionOutputs.get(0).getOutputs();
        assertEquals(2, outputs.size());
        assertEquals(1.0, outputs.get(0).getValue().asNumber());
        assertEquals(3.0, outputs.get(1).getValue().asNumber());

        assertEquals(customOutputNames, outputs.stream().map(Output::getName).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("Test inference with batch input")
    void testBatchInput() throws ExecutionException, InterruptedException {
        final Random random = new Random();
        KServeConfig config = KServeConfig.create("localhost:" + mockServer.getPort(), "test", "1");
        final List<String> customOutputNames = List.of("custom-output-a", "custom-output-b");
        PredictionProvider model = KServeV2GRPCPredictionProvider.forTarget(config, "test-input", customOutputNames, null);
        final int batchSize = 10;
        final List<PredictionInput> predictionInputs = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            predictionInputs.add(
                    new PredictionInput(List.of(
                            FeatureFactory.newNumericalFeature("d", random.nextDouble()),
                            FeatureFactory.newNumericalFeature("e", random.nextDouble()),
                            FeatureFactory.newNumericalFeature("f", random.nextDouble()))));
        }
        List<PredictionOutput> predictionOutputs = model.predictAsync(predictionInputs).get();

        assertEquals(10, predictionOutputs.size());
        final List<Output> outputs = predictionOutputs.get(0).getOutputs();
        assertEquals(2, outputs.size());
        assertEquals(customOutputNames, outputs.stream().map(Output::getName).collect(Collectors.toList()));
    }
}
