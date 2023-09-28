package org.kie.trustyai.service.data.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.payloads.service.Schema;
import org.kie.trustyai.service.payloads.service.SchemaItem;
import org.kie.trustyai.service.payloads.values.DataType;

import static org.junit.jupiter.api.Assertions.*;

class MetadataTest {
    private Map<String, SchemaItem> generateSchema(int n, double valueOffset, boolean makeNulls) {
        Map<String, SchemaItem> out = new HashMap<>();
        for (int i = 0; i < n; i++) {
            Set<Object> values;
            if (i % 2 == 0 && makeNulls) {
                values = null;
            } else {
                values = new HashSet<>();
                values.add(i + valueOffset);
            }
            SchemaItem row = new SchemaItem(DataType.DOUBLE, Integer.toString(i), values, i);
            out.put(Integer.toString(i), row);
        }
        return out;
    }

    @Test
    void validateNullValueMerge() {
        Schema s1 = Schema.from(generateSchema(10, .1, true));
        Schema s2 = Schema.from(generateSchema(10, .2, false));
        Metadata m1 = new Metadata();
        m1.setInputSchema(s1);
        m1.setOutputSchema(s2);

        m1.mergeInputSchema(s2);
        m1.mergeOutputSchema(s1);

        // check for merging null, nonnull
        for (Map.Entry<String, SchemaItem> entry : m1.getInputSchema().getItems().entrySet()) {
            int idx = Integer.parseInt(entry.getKey());
            //if null
            if (idx % 2 == 0) {
                assertNull(entry.getValue().getValues());
            } else {
                assertTrue(entry.getValue().getValues().contains(idx + .1));
                assertTrue(entry.getValue().getValues().contains(idx + .2));
            }
        }

        // check for merging nonnull, null
        for (Map.Entry<String, SchemaItem> entry : m1.getOutputSchema().getItems().entrySet()) {
            int idx = Integer.parseInt(entry.getKey());

            if (idx % 2 == 0) {
                assertNull(entry.getValue().getValues());
            } else {
                assertTrue(entry.getValue().getValues().contains(idx + .1));
                assertTrue(entry.getValue().getValues().contains(idx + .2));
            }
        }

    }
}
