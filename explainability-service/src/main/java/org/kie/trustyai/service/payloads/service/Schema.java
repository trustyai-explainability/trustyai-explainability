package org.kie.trustyai.service.payloads.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Schema {
    private final Map<String, SchemaItem> items;
    private Map<String, String> nameMapping = new HashMap<>();
    private Map<String, SchemaItem> mappedItems = new HashMap<>();

    public Schema() {
        this.items = new ConcurrentHashMap<>();
        calculateNameMappedItems();
    }

    public Schema(Map<String, SchemaItem> items) {
        this.items = items;
        calculateNameMappedItems();
    }

    public Schema(Map<String, SchemaItem> items, Map<String, String> nameMapping) {
        this.items = items;
        this.nameMapping = nameMapping;
        calculateNameMappedItems();
    }

    public static Schema from(Map<String, SchemaItem> items) {
        return new Schema(items);
    }

    public Map<String, SchemaItem> getItems() {
        return items;
    }

    //@CacheResult(cacheName = "schema-name-mapped-items", keyGenerator = SchemaNameMappingCacheKeyGen.class)
    private void calculateNameMappedItems() {
        this.mappedItems = new HashMap<>(items);
        if (!nameMapping.isEmpty()) {
            for (Map.Entry<String, String> mapping : nameMapping.entrySet()) {
                if (items.containsKey(mapping.getKey())) {
                    this.mappedItems.put(mapping.getKey(), items.get(mapping.getKey()));
                }
            }
        }
    }

    public Map<String, String> getNameMapping() {
        return nameMapping;
    }

    public void setNameMapping(Map<String, String> nameMapping) {
        this.nameMapping = nameMapping;
        calculateNameMappedItems();
    }

    public Map<String, SchemaItem> getNameMappedItems() {
        return mappedItems;
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
        return Objects.hash(items, nameMapping);
    }

    @Override
    public String toString() {
        return "Schema{" +
                "items=" + items +
                ", nameMapping=" + nameMapping +
                '}';
    }
}
