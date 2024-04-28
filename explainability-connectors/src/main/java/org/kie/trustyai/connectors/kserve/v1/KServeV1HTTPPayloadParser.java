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
                features.add(new Feature(DEFAULT_INPUT_PREFIX + "-" + (i + 1), Type.NUMBER, new Value(featureValues.get(i))));
            }
            predictionInputs.add(new PredictionInput(features));
        }

        return predictionInputs;
    }

    @Override
    public List<PredictionOutput> parseResponse(String jsonResponse) throws JsonProcessingException {
        logger.debug("Parsing response " + jsonResponse);
        final KServeV1ResponsePayload response = mapper.readValue(jsonResponse, KServeV1ResponsePayload.class);
        final List<PredictionOutput> predictionOutputs = new ArrayList<>();

        for (Object prediction : response.getPredictions()) {
            if (prediction instanceof List) {
                // Iterate over each element in the list, treating each as a separate PredictionOutput
                for (Object value : (List<?>) prediction) {
                    final List<Output> outputs = new ArrayList<>();
                    if (value instanceof Integer) {
                        outputs.add(new Output(DEFAULT_OUTPUT_PREFIX + "-0", Type.NUMBER, new Value((int) value), 1.0));
                    } else {
                        outputs.add(new Output(DEFAULT_OUTPUT_PREFIX + "-0", Type.NUMBER, new Value((double) value), 1.0));
                    }
                    predictionOutputs.add(new PredictionOutput(outputs));
                }
            } else {
                // Handle non-list single values by creating one PredictionOutput per value
                final List<Output> outputs = new ArrayList<>();
                if (prediction instanceof Integer) {
                    outputs.add(new Output(DEFAULT_OUTPUT_PREFIX, Type.NUMBER, new Value((int) prediction), 1.0));
                } else {
                    outputs.add(new Output(DEFAULT_OUTPUT_PREFIX, Type.NUMBER, new Value((double) prediction), 1.0));
                }
                predictionOutputs.add(new PredictionOutput(outputs));
            }
        }

        return predictionOutputs;
    }

}
