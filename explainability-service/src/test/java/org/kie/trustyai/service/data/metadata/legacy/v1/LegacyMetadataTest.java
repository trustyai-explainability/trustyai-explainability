package org.kie.trustyai.service.data.metadata.legacy.v1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.*;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.model.dataframe.DataframeMetadata;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;


import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
public class LegacyMetadataTest {

    @Inject
    Instance<MockMemoryStorage> storage;

    @Inject
    Instance<MockDatasource> datasource;

    private static final String MODEL_ID = "demo-loan-nn-onnx-beta";

    private static StorageMetadata readMetadataFile(String filename) throws IOException {

        final String resourcePath = "storage/data/v1/" + filename;

        // Create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("file not found! " + resourcePath);
            }

            return objectMapper.readValue(is, StorageMetadata.class);
        }

    }

    private static ByteBuffer readFile(String filename) throws IOException {
        String resourcePath = "storage/data/v1/" + filename;

        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("file not found! " + resourcePath);
            }

            // Read the InputStream into a String
            String content = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n")) + "\n";

            // Convert the String to a ByteBuffer
            return ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));

        }
    }

    @BeforeEach
    void emptyStorage() throws IOException {
        storage.get().emptyStorage();
        storage.get().saveDataframe(readFile("demo-loan-nn-onnx-beta-data.csv"), "demo-loan-nn-onnx-beta-data.csv");
        storage.get().saveDataframe(readFile("demo-loan-nn-onnx-beta-internal_data.csv"), "demo-loan-nn-onnx-beta-internal_data.csv");
        datasource.get().saveMetadata(readMetadataFile("demo-loan-nn-onnx-beta-metadata.json"), "demo-loan-nn-onnx-beta");
    }

    @Test
    void unchangedMetadata() {
        Dataframe df = datasource.get().getDataframe(MODEL_ID);

        assertEquals(DataframeMetadata.DEFAULT_INPUT_TENSOR_NAME, df.getInputTensorName());
        assertEquals(DataframeMetadata.DEFAULT_OUTPUT_TENSOR_NAME, df.getOutputTensorName());

        // Remove metadata
        final Prediction newPrediction = new SimplePrediction(new PredictionInput(df.getInputRowAsFeature(0)), new PredictionOutput(df.getOutputRowAsOutput(0)));
        final Dataframe changedDataframe = Dataframe.createFrom(newPrediction);
        changedDataframe.setInputTensorName("test-input-a");
        changedDataframe.setOutputTensorName("test-input-b");

        datasource.get().saveDataframe(changedDataframe, MODEL_ID);

        final Dataframe loadedDataframe = datasource.get().getDataframe(MODEL_ID);

        // Old metadata not changed
        assertEquals(DataframeMetadata.DEFAULT_INPUT_TENSOR_NAME, loadedDataframe.getInputTensorName());
        assertEquals(DataframeMetadata.DEFAULT_OUTPUT_TENSOR_NAME, loadedDataframe.getOutputTensorName());

    }
}
