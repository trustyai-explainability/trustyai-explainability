package org.kie.trustyai.service.payloads.data.download.serializers;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.kie.trustyai.service.payloads.data.download.DataRequestPayload;

import java.io.IOException;

public class DataRequestDeserializer extends StdDeserializer<DataRequestPayload> {
    public DataRequestDeserializer() {
        this(null);
    }

    public DataRequestDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public DataRequestPayload deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        return mapper.readValue(node.get("value").textValue(), DataRequestPayload.class);
    }
}
