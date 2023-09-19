package org.kie.trustyai.service.endpoints.consumer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockKServeInputPayload;
import org.kie.trustyai.service.mocks.MockKServeOutputPayload;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.consumer.InferenceLoggerOutput;

import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
public class CloudEventConsumerTest {

    final static String MODEL_NAME = "someModelId";
    @Inject
    Instance<CloudEventConsumer> consumer;
    @Inject
    Instance<MockDatasource> datasource;
    @Inject
    Instance<MockMemoryStorage> storage;

    @BeforeEach
    void emptyStorage() {
        datasource.get().empty();
        storage.get().emptyStorage();
    }

    private CloudEvent<byte[]> generateMockInput(String id) {
        final String payload = "{\"instances\":[[" + Math.random() + ", " + Math.random() + ", " + Math.random() + ", " + Math.random() + "]]}";
        return MockKServeInputPayload.create(id, payload.getBytes(StandardCharsets.UTF_8), MODEL_NAME);
    }

    private CloudEvent<InferenceLoggerOutput> generateMockOutput(String id) {
        InferenceLoggerOutput ilo = new InferenceLoggerOutput();
        ilo.setPredictions(List.of(Math.random()));
        return MockKServeOutputPayload.create(id, ilo, MODEL_NAME);
    }

    @Test
    @DisplayName("Valid Kubeflow CloudEvents should be stored")
    public void testConsumeKubeflowCloudEvents() {
        final String id = UUID.randomUUID().toString();
        CloudEvent<byte[]> mockInput = MockKServeInputPayload.create(id, "{\"instances\":[[40, 3.5, 0.5, 0]]}".getBytes(StandardCharsets.UTF_8), MODEL_NAME);

        consumer.get().consumeKubeflowRequest(mockInput);

        assertFalse(storage.get().dataExists(MODEL_NAME));

        InferenceLoggerOutput ilo = new InferenceLoggerOutput();
        ilo.setPredictions(List.of(1.0));

        CloudEvent<InferenceLoggerOutput> mockOutput = MockKServeOutputPayload.create(id, ilo, MODEL_NAME);
        consumer.get().consumeKubeflowResponse(mockOutput);

        assertTrue(storage.get().dataExists(MODEL_NAME));

        final Dataframe df = datasource.get().getDataframe(MODEL_NAME);
        assertEquals(5, df.getColumnDimension());
        assertEquals(4, df.getInputsCount());
        assertEquals(1, df.getOutputsCount());

        System.out.println("s");
    }

    @Test
    @DisplayName("Invalid Kubeflow CloudEvents should throw exception")
    public void testConsumeInvalidKubeflowCloudEvents() {
        final String id = UUID.randomUUID().toString();
        CloudEvent<byte[]> mockEvent = MockKServeInputPayload.create(id, "foo".getBytes(StandardCharsets.UTF_8), MODEL_NAME);
        consumer.get().consumeKubeflowRequest(mockEvent);
        InferenceLoggerOutput ilo = new InferenceLoggerOutput();
        ilo.setPredictions(List.of(1.0));
        CloudEvent<InferenceLoggerOutput> mockOutput = MockKServeOutputPayload.create(id, ilo, MODEL_NAME);

        final DataframeCreateException exception = assertThrows(DataframeCreateException.class, () -> {
            consumer.get().consumeKubeflowResponse(mockOutput);
        });

        assertEquals("Could not parse input data", exception.getMessage());
    }

    @Test
    @DisplayName("Concurrent cloud events should be processed correctly")
    public void testConsumeKubeflowCloudEventsConcurrently() {
        final int NUM_EVENTS = 1000;
        final ExecutorService executor = Executors.newFixedThreadPool(10);

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

                final CloudEvent<InferenceLoggerOutput> mockOutput = generateMockOutput(id);
                consumer.get().consumeKubeflowResponse(mockOutput);
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

        assertTrue(storage.get().dataExists(MODEL_NAME));
        final Dataframe df = datasource.get().getDataframe(MODEL_NAME);
        assertEquals(5, df.getColumnDimension());
        assertEquals(4, df.getInputsCount());
        assertEquals(1, df.getOutputsCount());
        assertEquals(NUM_EVENTS, df.getRowDimension());
    }
}
