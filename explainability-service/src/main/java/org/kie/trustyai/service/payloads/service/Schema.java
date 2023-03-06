package org.kie.trustyai.service.payloads.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Schema {
    private final List<SchemaItem> items;

    public Schema() {
        this.items = new ArrayList<>();
    }

    public Schema(List<SchemaItem> items) {
        this.items = items;
    }

    public static Schema from(List<SchemaItem> items) {
        return new Schema(items);
    }

    public List<SchemaItem> getItems() {
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
