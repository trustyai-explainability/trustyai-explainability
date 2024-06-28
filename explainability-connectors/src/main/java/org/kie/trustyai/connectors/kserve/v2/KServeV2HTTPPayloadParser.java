package org.kie.trustyai.connectors.kserve.v2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.connectors.kserve.KServeDatatype;
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

        final KServeV2RequestPayload.Inputs singleInput = payload.inputs.get(0);
        // Process each list of instances as a separate PredictionInput
        for (List<Object> featureValues : singleInput.data) {
            final List<Feature> features = new ArrayList<>();
            final KServeDatatype datatype = singleInput.datatype;
            for (int i = 0; i < featureValues.size(); i++) {
                features.add(getFeature(datatype, i, featureValues.get(i)));
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
        final KServeDatatype datatype = koutput.datatype;

        for (int i = 0; i < numPredictions; i += shape) {
            final List<Output> outputs = new ArrayList<>();

            for (int j = 0; j < outputShape && i + j < numPredictions; j++) {
                final Object value = predictions.get(i + j);

                final Output output = getOutput(datatype, j, value);
                outputs.add(output);
            }

            predictionOutputs.add(new PredictionOutput(outputs));
        }

        return predictionOutputs;
    }

    public static Output getOutput(KServeDatatype datatype, int j, Object value) {
        Output output;
        if (datatype == KServeDatatype.FP32 || datatype == KServeDatatype.FP64 || datatype == KServeDatatype.INT8 || datatype == KServeDatatype.INT16 || datatype == KServeDatatype.INT32 || datatype == KServeDatatype.INT64) {
            output = new Output(DEFAULT_OUTPUT_PREFIX + "-" + j, Type.NUMBER, new Value(value), 1.0);
        } else if (datatype == KServeDatatype.BOOL) {
            output = new Output(DEFAULT_OUTPUT_PREFIX + "-" + j, Type.BOOLEAN, new Value(value), 1.0);
        } else  {
            output = new Output(DEFAULT_OUTPUT_PREFIX + "-" + j, Type.CATEGORICAL, new Value(value), 1.0);
        }
        return output;
    }

    public static Feature getFeature(KServeDatatype datatype, int j, Object value) {
        Feature feature;
        if (datatype == KServeDatatype.FP32 || datatype == KServeDatatype.FP64 || datatype == KServeDatatype.INT8 || datatype == KServeDatatype.INT16 || datatype == KServeDatatype.INT32 || datatype == KServeDatatype.INT64) {
            feature = new Feature(DEFAULT_OUTPUT_PREFIX + "-" + j, Type.NUMBER, new Value(value));
        } else if (datatype == KServeDatatype.BOOL) {
            feature = new Feature(DEFAULT_OUTPUT_PREFIX + "-" + j, Type.BOOLEAN, new Value(value));
        } else  {
            feature = new Feature(DEFAULT_OUTPUT_PREFIX + "-" + j, Type.CATEGORICAL, new Value(value));
        }
        return feature;
    }

}
