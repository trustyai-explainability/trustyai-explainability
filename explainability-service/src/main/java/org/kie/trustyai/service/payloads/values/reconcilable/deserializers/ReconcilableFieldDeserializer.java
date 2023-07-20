package org.kie.trustyai.service.payloads.values.reconcilable.deserializers;

import java.io.IOException;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.TypedValue;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableFeature;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableField;
import org.kie.trustyai.service.payloads.values.reconcilable.ReconcilableOutput;
import org.kie.trustyai.service.prometheus.PrometheusPublisher;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ValueNode;

// deserializers ReconcilableFields; is the inverse of the ReconcilableFieldSerializer
public class ReconcilableFieldDeserializer extends StdDeserializer<ReconcilableField> {
    public ReconcilableFieldDeserializer() {
        this(null);
    }

    private static final Logger LOG = Logger.getLogger(PrometheusPublisher.class);

    public ReconcilableFieldDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ReconcilableField deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        LOG.info("trying to deserialize: " + node);
        ReconcilableField rf;
        JsonNode value = node.get("value");
        LOG.info("deserialize value:" + value);

        if (node.get("category").asText().equals("feature")) {
            rf = new ReconcilableFeature((ValueNode) value);
        } else {
            rf = new ReconcilableOutput((ValueNode) value);
        }

        if (!node.get("type").asText().equals("null")) {
            String type = node.get("type").asText();
            TypedValue tv = new TypedValue();
            tv.setValue(value);
            tv.setType(Enum.valueOf(DataType.class, type));
            rf.setReconciledType(Optional.of(tv));
        } else {
            rf.setReconciledType(Optional.empty());
        }
        return rf;
    }
}
