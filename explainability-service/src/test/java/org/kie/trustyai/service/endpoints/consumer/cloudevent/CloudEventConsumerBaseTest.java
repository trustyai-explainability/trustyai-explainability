package org.kie.trustyai.service.endpoints.consumer.cloudevent;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.SerializableObject;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.endpoints.consumer.CloudEventConsumer;
import org.kie.trustyai.service.mocks.kserve.MockKServeInputPayload;
import org.kie.trustyai.service.mocks.kserve.MockKServeOutputPayload;
import org.kie.trustyai.service.payloads.consumer.InferenceLoggerOutput;
import org.kie.trustyai.service.payloads.consumer.InferenceLoggerOutputObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static io.restassured.RestAssured.given;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
abstract public class CloudEventConsumerBaseTest {

    final static String MODEL_NAME = "someModelId";
    @Inject
    Instance<CloudEventConsumer> consumer;

    @Inject
    Instance<DataSource> datasource;

    abstract void resetDatasource() throws JsonProcessingException;

    abstract void clearStorage() throws JsonProcessingException;

    abstract Storage getStorage();

    @BeforeEach
    void emptyStorage() throws JsonProcessingException {
        resetDatasource();
        clearStorage();
    }

    private static final String input = "{\n" +
            "    \"inputs\": [\n" +
            "        {\n" +
            "            \"name\": \"inputs\",\n" +
            "            \"shape\": [\n" +
            "                5,\n" +
            "                2\n" +
            "            ],\n" +
            "            \"datatype\": \"FP64\",\n" +
            "            \"data\": [\n" +
            "                [\n" +
            "                    8.3252,\n" +
            "                    41.0\n" +
            "                ],\n" +
            "                [\n" +
            "                    8.3014,\n" +
            "                    21.0\n" +
            "                ],\n" +
            "                [\n" +
            "                    7.2574,\n" +
            "                    52.0\n" +
            "                ],\n" +
            "                [\n" +
            "                    5.6431,\n" +
            "                    52.0\n" +
            "                ],\n" +
            "                [\n" +
            "                    3.8462,\n" +
            "                    52.0\n" +
            "                ]\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}\n";

    private static final String output = "{\n" +
            "    \"model_name\": \"explainer-test-a\",\n" +
            "    \"id\": \"29816015-fc5b-4226-98dc-344c190c03bf\",\n" +
            "    \"parameters\": {},\n" +
            "    \"outputs\": [\n" +
            "        {\n" +
            "            \"name\": \"predict\",\n" +
            "            \"shape\": [\n" +
            "                5,\n" +
            "                1\n" +
            "            ],\n" +
            "            \"datatype\": \"FP64\",\n" +
            "            \"data\": [\n" +
            "                87.53267207528769,\n" +
            "                85.33306501426134,\n" +
            "                77.90240870728456,\n" +
            "                61.71289055261055,\n" +
            "                43.69211054137737\n" +
            "            ]\n" +
            "        }\n" +
            "    ]\n" +
            "}";

    private static final String largeInput = "{\n" +
            "  \"inputs\": [\n" +
            "    {\n" +
            "      \"name\": \"customer_data_input\",\n" +
            "      \"shape\": [%d, 5],\n" +
            "      \"datatype\": \"FP64\",\n" +
            "      \"data\": [\n%s\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String data = "        [1.0, 112500.0, 2.0, 1.0, 1.0]";

    private CloudEvent<byte[]> generateMockInput(String id) {
        final String payload = "{\"instances\":[[" + Math.random() + ", " + Math.random() + ", " + Math.random() + ", " + Math.random() + "]]}";
        return MockKServeInputPayload.create(id, payload.getBytes(StandardCharsets.UTF_8), MODEL_NAME);
    }

    private CloudEvent<byte[]> generateMockOutput(String id) throws JsonProcessingException {
        InferenceLoggerOutput ilo = new InferenceLoggerOutput();
        ilo.setPredictionsDouble(List.of(Math.random()));
        ObjectMapper mapper = new ObjectMapper();
        return MockKServeOutputPayload.create(id, mapper.writeValueAsBytes(ilo), MODEL_NAME);
    }

    private CloudEvent<byte[]> generateMockBatchInput(String id) {
        return MockKServeInputPayload.create(id, input.getBytes(StandardCharsets.UTF_8), MODEL_NAME);
    }

    private String generateMockLargeInput(String id, int n) {
        String content = (data + ",\n").repeat(n - 1) + data;
        String payload = String.format(largeInput, n, content);
        return payload;//.getBytes(StandardCharsets.UTF_8);
    }

    private CloudEvent<byte[]> generateMockBatchOutput(String id) throws JsonProcessingException {
        InferenceLoggerOutput ilo = new InferenceLoggerOutput();
        InferenceLoggerOutputObject iloo = new InferenceLoggerOutputObject();
        ilo.setPredictions(null);
        iloo.setData(Stream.of(
                87.53267207528769,
                85.33306501426134,
                77.90240870728456,
                61.71289055261055,
                43.69211054137737).map(SerializableObject::new).collect(Collectors.toList()));
        iloo.setDatatype("FP64");
        iloo.setName("predict");
        iloo.setShape(List.of(5, 1));
        ilo.setOutputs(List.of(iloo));
        ObjectMapper mapper = new ObjectMapper();
        return MockKServeOutputPayload.create(id, mapper.writeValueAsBytes(ilo), MODEL_NAME);
    }

    @Test
    @DisplayName("Valid Kubeflow CloudEvents should be stored")
    public void testConsumeKubeflowCloudEvents() throws JsonProcessingException {
        final String id = UUID.randomUUID().toString();
        CloudEvent<byte[]> mockInput = MockKServeInputPayload.create(id, "{\"instances\":[[40.0, 3.5, 0.5, 0.0]]}".getBytes(StandardCharsets.UTF_8), MODEL_NAME);

        consumer.get().consumeKubeflowRequest(mockInput);

        assertFalse(getStorage().dataExists(MODEL_NAME));

        InferenceLoggerOutput ilo = new InferenceLoggerOutput();
        ilo.setPredictions(List.of(new SerializableObject(1.0)));
        ObjectMapper mapper = new ObjectMapper();
        CloudEvent<byte[]> mockOutput = MockKServeOutputPayload.create(id, mapper.writeValueAsBytes(ilo), MODEL_NAME);
        consumer.get().consumeKubeflowResponse(mockOutput);

        assertTrue(getStorage().dataExists(MODEL_NAME));

        final Dataframe df = datasource.get().getDataframe(MODEL_NAME);
        assertEquals(5, df.getColumnDimension());
        assertEquals(4, df.getInputsCount());
        assertEquals(1, df.getOutputsCount());
    }

    @Test
    @DisplayName("Valid Batched Kubeflow CloudEvents should be stored")
    public void testConsumeBatchedKubeflowCloudEvents() throws JsonProcessingException {
        final String id = UUID.randomUUID().toString();
        CloudEvent<byte[]> mockInput = generateMockBatchInput(id);
        consumer.get().consumeKubeflowRequest(mockInput);
        assertFalse(getStorage().dataExists(MODEL_NAME));

        CloudEvent<byte[]> mockOutput = generateMockBatchOutput(id);
        consumer.get().consumeKubeflowResponse(mockOutput);
        assertTrue(getStorage().dataExists(MODEL_NAME));

        final Dataframe df = datasource.get().getDataframe(MODEL_NAME);
        assertEquals(3, df.getColumnDimension());
        assertEquals(5, df.getRowDimension());
        assertEquals(2, df.getInputsCount());
        assertEquals(1, df.getOutputsCount());
    }

    @Test
    @DisplayName("Large Kubeflow CloudEvents should be accepted")
    public void testConsumeLargeKubeflowCloudEvents() {
        final String id = UUID.randomUUID().toString();
        String mockInput = generateMockLargeInput(id, 50_000);
        System.out.println(mockInput.getBytes(StandardCharsets.UTF_8).length / 1000. / 1000. + "MB");
        given()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("ce-specversion", "1.0")
                .header("ce-id", "65b7fe9a-3ef7-4683-8b89-dad1cb5f1464")
                .header("ce-type", "org.kubeflow.serving.inference.request")
                .header("ce-source", "http://localhost:9081/")
                .header("ce-datacontenttype", "application/x-www-form-urlencoded")
                .body(mockInput)
                .when().post("/")
                .then()
                .statusCode(RestResponse.StatusCode.NO_CONTENT);
    }

    @Test
    @DisplayName("Invalid Kubeflow CloudEvents should throw exception")
    public void testConsumeInvalidKubeflowCloudEvents() throws JsonProcessingException {
        final String id = UUID.randomUUID().toString();
        CloudEvent<byte[]> mockEvent = MockKServeInputPayload.create(id, "foo".getBytes(StandardCharsets.UTF_8), MODEL_NAME);
        consumer.get().consumeKubeflowRequest(mockEvent);
        InferenceLoggerOutput ilo = new InferenceLoggerOutput();
        ilo.setPredictions(List.of(new SerializableObject(1.0)));
        ObjectMapper mapper = new ObjectMapper();
        CloudEvent<byte[]> mockOutput = MockKServeOutputPayload.create(id, mapper.writeValueAsBytes(ilo), MODEL_NAME);

        final DataframeCreateException exception = assertThrows(DataframeCreateException.class, () -> {
            consumer.get().consumeKubeflowResponse(mockOutput);
        });
        assertEquals("Could not parse input data: Unrecognized token 'foo': was expecting (JSON String, Number, Array, Object or token 'null', 'true' or 'false')\n" +
                " at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); line: 1, column: 4]",
                exception.getMessage());
    }

    @Test
    @DisplayName("Concurrent cloud events should be processed correctly")
    public void testConsumeKubeflowCloudEventsConcurrently() {
        final int NUM_EVENTS = 1000;
        ManagedExecutor executor = ManagedExecutor.builder().maxAsync(10).propagated(ThreadContext.CDI).build();

        IntStream.range(0, NUM_EVENTS).forEach(i -> {
            executor.submit(() -> {
                final String id = UUID.randomUUID().toString();
                final CloudEvent<byte[]> mockInput = generateMockInput(id);
                consumer.get().consumeKubeflowRequest(mockInput);

                try {
                    Thread.sleep((long) (Math.random() * 100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                final CloudEvent<byte[]> mockOutput;
                try {
                    mockOutput = generateMockOutput(id);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                try {
                    consumer.get().consumeKubeflowResponse(mockOutput);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            });
        });

        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                fail("Test time out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted");
        }

        assertTrue(getStorage().dataExists(MODEL_NAME));
        final Dataframe df = datasource.get().getDataframe(MODEL_NAME);
        assertEquals(5, df.getColumnDimension());
        assertEquals(4, df.getInputsCount());
        assertEquals(1, df.getOutputsCount());
        assertEquals(NUM_EVENTS, df.getRowDimension());
    }
}
