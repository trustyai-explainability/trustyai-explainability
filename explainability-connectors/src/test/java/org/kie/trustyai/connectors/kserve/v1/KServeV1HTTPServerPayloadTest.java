package org.kie.trustyai.connectors.kserve.v1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.utils.models.TestModels;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KServeV1HTTPServerPayloadTest {

    private static final String endpoint = "/v1/model/infer";
    private static int port = 50081;
    private KServeV1HTTPMockServer server;

    private static String generateRequestPayload(List<Object> values) throws JsonProcessingException {
        // Convert the payload to JSON string
        final ObjectMapper objectMapper = new ObjectMapper();

        return objectMapper.writeValueAsString(Map.of("instances", values));
    }

    private static HttpResponse<String> getResponse(String payload) throws IOException, InterruptedException {
        final HttpClient client = HttpClient.newHttpClient();

        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + endpoint))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .header(HttpHeaderNames.CONTENT_TYPE.toString(), HttpHeaderValues.APPLICATION_JSON.toString())
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static int getFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to find an available port", e);
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        port = getFreePort();
        server = new KServeV1HTTPMockServer(port, endpoint, null);
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
    }

    @Test
    void testSingleInput() throws Exception {
        server.setPredictionProvider(TestModels.getSumSkipModel(1));
        server.start();

        final String payload = generateRequestPayload(List.of(List.of(1.0, 2.0, 3.0)));
        final HttpResponse<String> response = getResponse(payload);

        assertEquals(200, response.statusCode());

        final List<PredictionOutput> predictionOutputs = KServeV1HTTPPayloadParser.getInstance().parseResponse(response.body());

        assertEquals(1, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        assertEquals(4.0, predictionOutputs.get(0).getOutputs().get(0).getValue().asNumber());
    }

    @Test
    void testMultiInput() throws Exception {
        server.setPredictionProvider(TestModels.getSumSkipModel(1));
        server.start();

        final String payload = generateRequestPayload(List.of(List.of(1.0, 2.0, 3.0), List.of(2.0, 1.0, 1.0)));
        final HttpResponse<String> response = getResponse(payload);

        assertEquals(200, response.statusCode());

        final List<PredictionOutput> predictionOutputs = KServeV1HTTPPayloadParser.getInstance().parseResponse(response.body());

        assertEquals(2, predictionOutputs.size());
        assertEquals(1, predictionOutputs.get(0).getOutputs().size());
        assertEquals(4.0, predictionOutputs.get(0).getOutputs().get(0).getValue().asNumber());
        assertEquals(3.0, predictionOutputs.get(1).getOutputs().get(0).getValue().asNumber());

        server.stop();
    }

    @Test
    void testMultiOutput() throws Exception {
        server.setPredictionProvider(TestModels.getFeatureSkipModel(1));
        server.start();

        final String payload = generateRequestPayload(List.of(List.of(1.0, 2.0, 3.0), List.of(2.0, 1.0, 1.0)));
        final HttpResponse<String> response = getResponse(payload);

        assertEquals(200, response.statusCode());

        final List<PredictionOutput> predictionOutputs = KServeV1HTTPPayloadParser.getInstance().parseResponse(response.body(), 2);

        assertEquals(2, predictionOutputs.size());
        assertEquals(Set.of(2), predictionOutputs.stream().map(po -> po.getOutputs().size()).collect(Collectors.toSet()));
        assertEquals(1.0, predictionOutputs.get(0).getOutputs().get(0).getValue().asNumber());
        assertEquals(2.0, predictionOutputs.get(1).getOutputs().get(0).getValue().asNumber());
    }

}
