package org.kie.trustyai.service.payloads.service.readable;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.service.payloads.service.Schema;
import org.kie.trustyai.service.payloads.service.SchemaItem;

public class ReadableSchema {
    private Map<String, ReadableSchemaItem> items;
    private Map<String, String> nameMapping;

    public ReadableSchema() {
    }

    public ReadableSchema(Schema s) {
        this.nameMapping = s.getNameMapping();
        this.items = new HashMap<>();
        for (Map.Entry<String, SchemaItem> entry : s.getNameMappedItems().entrySet()) {
            this.items.put(entry.getKey(), new ReadableSchemaItem(entry.getValue()));
        }
    }

    public Map<String, ReadableSchemaItem> getItems() {
        return items;
    }

    public Map<String, String> getNameMapping() {
        return nameMapping;
    }


    public void setItems(Map<String, ReadableSchemaItem> items) {
        this.items = items;
    }

    public void setNameMapping(Map<String, String> nameMapping) {
        this.nameMapping = nameMapping;
    }
}
