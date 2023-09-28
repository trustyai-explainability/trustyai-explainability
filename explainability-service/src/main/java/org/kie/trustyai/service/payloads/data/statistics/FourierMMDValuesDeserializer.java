package org.kie.trustyai.service.payloads.data.statistics;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class FourierMMDValuesDeserializer extends StdDeserializer<Map<String, Object>> {

    public FourierMMDValuesDeserializer() {
        this(null);
    };

    public FourierMMDValuesDeserializer(Class<Map<String, Object>> t) {
        super(t);
    };

    @Override
    public Map<String, Object> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Map<String, Object> result = new HashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> entry = it.next();
            result.put(entry.getKey(), parseJsonToObject(entry));
        }
        return result;
    }

    public double[] jsonNodeToDoubleArray(JsonNode node) {
        int size = node.size();
        double[] doubleArray = new double[size];
        for (int i = 0; i < size; i++) {
            doubleArray[i] = node.get(i).asDouble();
        }
        return doubleArray;
    }

    private Object parseJsonToObject(Map.Entry<String, JsonNode> entry) {
        if ("scale" == entry.getKey()) {
            return jsonNodeToDoubleArray(entry.getValue());
        } else if ("aRef" == entry.getKey()) {
            return jsonNodeToDoubleArray(entry.getValue());
        } else if ("mean_mmd" == entry.getKey()) {
            return entry.getValue().asDouble();
        } else if ("std_mmd" == entry.getKey()) {
            return entry.getValue().asDouble();
        } else if ("randomSeed" == entry.getKey()) {
            return entry.getValue().asInt();
        } else if ("deltaStat" == entry.getKey()) {
            return entry.getValue().asBoolean();
        } else if ("n_mode" == entry.getKey()) {
            return entry.getValue().asInt();
        } else {
            return entry.getValue();
        }
    }
}
