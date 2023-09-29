package org.kie.trustyai.service.payloads.data.statistics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Triple;
import org.kie.trustyai.metrics.drift.kstest.GKSketch;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class GKSketchesDeserializer extends StdDeserializer<Map<String, GKSketch>> {

    public GKSketchesDeserializer() {
        this(null);
    };

    public GKSketchesDeserializer(Class<Map<String, GKSketch>> t) {
        super(t);
    }

    @Override
    public Map<String, GKSketch> deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JacksonException {
        JsonNode node = jp.getCodec().readTree(jp);
        Map<String, GKSketch> result = new HashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> entry = it.next();
            GKSketch sketch = parseJsonToGKSketch(entry.getValue());
            sketch.setSummary(jsonNodeToTripleList(entry.getValue().get("summary")));
            result.put(entry.getKey(), sketch);
        }
        return result;
    };

    public List<Triple<Double, Integer, Integer>> jsonNodeToTripleList(JsonNode node) {
        int size = node.size();
        List<Triple<Double, Integer, Integer>> summary = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            summary.add(Triple.of(node.get(i).get("left").asDouble(), node.get(i).get("middle").asInt(), node.get(i).get("right").asInt()));
        }
        return summary;
    }

    private GKSketch parseJsonToGKSketch(JsonNode node) {
        return new GKSketch(
                node.get("epsilon").asDouble(),
                node.get("xmin").asDouble(),
                node.get("xmax").asDouble(),
                node.get("numx").asInt());
    }
}
