package org.kie.trustyai.service.payloads.service;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Schema {
    private final Map<String, SchemaItem> items;

    public Schema() {
        this.items = new ConcurrentHashMap<>();
    }

    public Schema(Map<String, SchemaItem> items) {
        this.items = items;
    }

    public static Schema from(Map<String, SchemaItem> items) {
        return new Schema(items);
    }

    public Map<String, SchemaItem> getItems() {
        return items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Schema schema = (Schema) o;
        return items.equals(schema.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items);
    }
}
