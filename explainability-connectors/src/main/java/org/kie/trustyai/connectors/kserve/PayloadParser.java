package org.kie.trustyai.connectors.kserve;

import java.io.IOException;
import java.util.List;

import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;

import com.fasterxml.jackson.core.JsonProcessingException;

public abstract class PayloadParser<T> {
    public abstract List<PredictionInput> parseRequest(T request) throws IOException;

    public abstract List<PredictionOutput> parseResponse(T response, int outputShape) throws JsonProcessingException;

    public List<PredictionOutput> parseResponse(T response) throws JsonProcessingException {
        return parseResponse(response, 1);
    }
}
