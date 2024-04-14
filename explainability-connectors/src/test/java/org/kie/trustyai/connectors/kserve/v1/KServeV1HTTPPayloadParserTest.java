package org.kie.trustyai.connectors.kserve.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KServeV1HTTPPayloadParserTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String toJson(List<Object> predictions) {
        try {
            KServeV1ResponsePayload payload = new KServeV1ResponsePayload();
            payload.setPredictions(predictions);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize predictions to JSON", e);
        }
    }

    public static String generateJSONInputs(int inputs, int features) throws JsonProcessingException {
        final Random random = new Random(0);

        final List<List<Double>> inputList = new ArrayList<>();
        for (int i = 0; i < inputs; i++) {
            final List<Double> featureList = new ArrayList<>();
            for (int j = 0; j < features; j++) {
                featureList.add(random.nextDouble());
            }
            inputList.add(featureList);
        }
        final KServeV1RequestPayload payload = new KServeV1RequestPayload(inputList);

        return objectMapper.writeValueAsString(payload);
    }

    @Test
    void modelResponseToPredictionOutputSingle() throws JsonProcessingException {

        final Random random = new Random(0);
        final double value = random.nextDouble();

        final String json = toJson(List.of(value));

        final List<PredictionOutput> predictionOutput = KServeV1HTTPPayloadParser.getInstance().parseResponse(json);

        assertEquals(1, predictionOutput.size());
        assertEquals(1, predictionOutput.get(0).getOutputs().size());
    }

    @Test
    void modelResponseToPredictionOutputMulti() throws JsonProcessingException {

        final Random random = new Random(0);
        final List<Object> values = random.doubles(3).boxed().collect(Collectors.toList());

        final String json = toJson(values);

        final List<PredictionOutput> predictionOutput = KServeV1HTTPPayloadParser.getInstance().parseResponse(json);

        assertEquals(3, predictionOutput.size());
        assertEquals(1, predictionOutput.get(0).getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), predictionOutput.get(i).getOutputs().get(0).getValue().asNumber());
        }
    }

    @Test
    void modelResponseToPredictionOutputFp32() throws JsonProcessingException {

        final Random random = new Random(0);
        final List<Object> values = List.of(random.nextFloat(), random.nextFloat(), random.nextFloat());
        final String json = toJson(values);

        final List<PredictionOutput> predictionOutput = KServeV1HTTPPayloadParser.getInstance().parseResponse(json);

        assertEquals(3, predictionOutput.size());
        assertEquals(1, predictionOutput.get(0).getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), Double.valueOf(predictionOutput.get(i).getOutputs().get(0).getValue().asNumber()).floatValue());
        }
    }

    @Test
    void modelResponseToPredictionOutputFp64() throws JsonProcessingException {

        final Random random = new Random(0);
        final List<Object> values = List.of(random.nextDouble(), random.nextDouble(), random.nextDouble());
        final String json = toJson(values);

        final List<PredictionOutput> predictionOutput = KServeV1HTTPPayloadParser.getInstance().parseResponse(json);

        assertEquals(3, predictionOutput.size());
        assertEquals(1, predictionOutput.get(0).getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), predictionOutput.get(i).getOutputs().get(0).getValue().asNumber());
        }
    }


    @Test
    void modelSingleToRequestToPredictionInput() throws IOException {

        final String json = generateJSONInputs(1, 1);

        final List<PredictionInput> inputs = KServeV1HTTPPayloadParser.getInstance().parseRequest(json);

        assertEquals(1, inputs.size());
        assertEquals(1, inputs.get(0).getFeatures().size());
    }

    @Test
    void modelRequestToPredictionInputMulti() throws IOException {

        final String json = generateJSONInputs(1, 3);

        final List<PredictionInput> inputs = KServeV1HTTPPayloadParser.getInstance().parseRequest(json);

        assertEquals(1, inputs.size());
        assertEquals(3, inputs.get(0).getFeatures().size());
    }

    @Test
    void modelRequestToPredictionInputFp32() throws IOException {

        final String json = generateJSONInputs(3, 1);

        final List<PredictionInput> inputs = KServeV1HTTPPayloadParser.getInstance().parseRequest(json);

        assertEquals(3, inputs.size());
        assertEquals(1, inputs.get(0).getFeatures().size());
    }



}