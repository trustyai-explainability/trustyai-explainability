package org.kie.trustyai.connectors.kserve.v1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.connectors.kserve.PayloadParser;
import org.kie.trustyai.explainability.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KServeV1HTTPPayloadParser extends PayloadParser<String> {
    private static final Logger logger = LoggerFactory.getLogger(KServeV1HTTPPayloadParser.class);
    private static KServeV1HTTPPayloadParser instance;
    private ObjectMapper mapper;

    private KServeV1HTTPPayloadParser() {
        this.mapper = new ObjectMapper();
    }

    public static synchronized KServeV1HTTPPayloadParser getInstance() {
        if (instance == null) {
            instance = new KServeV1HTTPPayloadParser();
        }
        return instance;
    }

    @Override
    public List<PredictionInput> parseRequest(String json) throws IOException {
        final KServeV1RequestPayload payload = mapper.readValue(json, KServeV1RequestPayload.class);
        final List<PredictionInput> predictionInputs = new ArrayList<>();

        // Process each list of instances as a separate PredictionInput
        for (List<Double> featureValues : payload.instances) {
            final List<Feature> features = new ArrayList<>();
            for (int i = 0; i < featureValues.size(); i++) {
                features.add(new Feature("Feature" + (i + 1), Type.NUMBER, new Value(featureValues.get(i))));
            }
            predictionInputs.add(new PredictionInput(features));
        }

        return predictionInputs;
    }

    @Override
    public List<PredictionOutput> parseResponse(String jsonResponse, int outputShape) throws JsonProcessingException {
        logger.debug("Parsing response " + jsonResponse);
        final KServeV1ResponsePayload response = mapper.readValue(jsonResponse, KServeV1ResponsePayload.class);
        final List<PredictionOutput> predictionOutputs = new ArrayList<>();

        final List<?> predictions = response.getPredictions();
        final int numPredictions = predictions.size();

        for (int i = 0; i < numPredictions; i += outputShape) {
            final List<Output> outputs = new ArrayList<>();

            for (int j = 0; j < outputShape && i + j < numPredictions; j++) {
                final Object value = predictions.get(i + j);

                if (value instanceof Integer) {
                    outputs.add(new Output("value", Type.NUMBER, new Value((int) value), 1.0));
                } else {
                    outputs.add(new Output("value", Type.NUMBER, new Value((double) value), 1.0));
                }
            }

            predictionOutputs.add(new PredictionOutput(outputs));
        }

        return predictionOutputs;
    }

}
