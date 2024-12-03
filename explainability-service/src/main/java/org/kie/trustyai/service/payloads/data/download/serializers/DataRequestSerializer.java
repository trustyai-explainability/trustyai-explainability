package org.kie.trustyai.service.payloads.data.download.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.kie.trustyai.service.payloads.data.download.DataRequestPayload;
import org.kie.trustyai.service.payloads.values.DataType;

import java.io.IOException;

public class DataRequestSerializer extends StdSerializer<DataRequestPayload>{
    public DataRequestSerializer() {
        this(null);
    }

    public DataRequestSerializer(Class<DataRequestPayload> t) {
        super(t);
    }

    @Override
    public void serialize(DataRequestPayload dataRequestPayload, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("type", DataType.MAP.toString());
        jsonGenerator.writeStringField("value", new ObjectMapper().writer().writeValueAsString(dataRequestPayload));
        jsonGenerator.writeEndObject();
    }
}


