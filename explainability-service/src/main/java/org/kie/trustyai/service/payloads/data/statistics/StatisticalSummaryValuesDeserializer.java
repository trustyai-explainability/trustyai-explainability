package org.kie.trustyai.service.payloads.data.statistics;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;


import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class StatisticalSummaryValuesDeserializer extends StdDeserializer<Map<String, StatisticalSummaryValues>> {

    public StatisticalSummaryValuesDeserializer(){ this(null); };

    public StatisticalSummaryValuesDeserializer(Class<Map<String, StatisticalSummaryValues>> t){ super(t); };

    @Override
    public Map<String, StatisticalSummaryValues> deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        Map<String, StatisticalSummaryValues> result = new HashMap<>();

        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            result.put(entry.getKey(), parseJsonToSSV(entry.getValue()));
        }
        return result;
    }

    private StatisticalSummaryValues parseJsonToSSV(JsonNode node){
        return new StatisticalSummaryValues(
                node.get("mean").asDouble(),
                node.get("variance").asDouble(),
                node.get("n").asLong(),
                node.get("max").asDouble(),
                node.get("min").asDouble(),
                node.get("sum").asDouble()
        );
    }
}
