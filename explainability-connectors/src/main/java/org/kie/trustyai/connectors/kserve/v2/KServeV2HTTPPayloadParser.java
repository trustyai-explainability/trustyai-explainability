package org.kie.trustyai.connectors.kserve.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.connectors.kserve.PayloadParser;
import org.kie.trustyai.explainability.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class KServeV2HTTPPayloadParser extends PayloadParser<String> {
    private static final Logger logger = LoggerFactory.getLogger(KServeV2HTTPPayloadParser.class);
    private static KServeV2HTTPPayloadParser instance;
    private final ObjectMapper mapper;

    private KServeV2HTTPPayloadParser() {
        this.mapper = new ObjectMapper();
    }

    public static synchronized KServeV2HTTPPayloadParser getInstance() {
        if (instance == null) {
            instance = new KServeV2HTTPPayloadParser();
        }
        return instance;
    }

    @Override
    public List<PredictionInput> parseRequest(String json) throws IOException {
        final KServeV2RequestPayload payload = mapper.readValue(json, KServeV2RequestPayload.class);
        final List<PredictionInput> predictionInputs = new ArrayList<>();

        // Process each list of instances as a separate PredictionInput
        for (List<Double> featureValues : payload.inputs.get(0).data) {
            final List<Feature> features = new ArrayList<>();
            for (int i = 0; i < featureValues.size(); i++) {
                features.add(new Feature(DEFAULT_INPUT_PREFIX + "-" + i, Type.NUMBER, new Value(featureValues.get(i))));
            }
            predictionInputs.add(new PredictionInput(features));
        }

        return predictionInputs;
    }

    @Override
    public List<PredictionOutput> parseResponse(String jsonResponse, int outputShape) throws JsonProcessingException {
        logger.debug("Parsing response " + jsonResponse);
        final KServeV2ResponsePayload response = mapper.readValue(jsonResponse, KServeV2ResponsePayload.class);
        final List<PredictionOutput> predictionOutputs = new ArrayList<>();

        final KServeV2ResponsePayload.Outputs koutput = response.getOutputs().get(0);

        final List<?> predictions = koutput.data;
        final int shape = koutput.shape.get(1);
        final int numPredictions = predictions.size();

        for (int i = 0; i < numPredictions; i += shape) {
            final List<Output> outputs = new ArrayList<>();

            for (int j = 0; j < outputShape && i + j < numPredictions; j++) {
                final Object value = predictions.get(i + j);

                if (value instanceof Integer) {
                    outputs.add(new Output(DEFAULT_OUTPUT_PREFIX + "-" + j, Type.NUMBER, new Value(value), 1.0));
                } else {
                    outputs.add(new Output(DEFAULT_OUTPUT_PREFIX + "-" + j, Type.NUMBER, new Value(value), 1.0));
                }
            }

            predictionOutputs.add(new PredictionOutput(outputs));
        }

        return predictionOutputs;
    }

}
