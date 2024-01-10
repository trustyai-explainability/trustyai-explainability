package org.kie.trustyai.service.payloads.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;

public class Schema {
    private static final Logger LOG = Logger.getLogger(Schema.class);

    private final Map<String, SchemaItem> items;

    public int getRemapCount() {
        return remapCount;
    }

    public void setRemapCount(int remapCount) {
        this.remapCount = remapCount;
    }

    private int remapCount = 0;
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
            this.remapCount++;
            // for key, value pair in the name mapping
            for (Map.Entry<String, String> mapping : nameMapping.entrySet()) {

                // if there is a corresponding key in the raw field names
                if (items.containsKey(mapping.getKey())) {
                    // swap the raw field name for the mapped field name
                    this.mappedItems.remove(mapping.getKey());
                    this.mappedItems.put(mapping.getValue(), items.get(mapping.getKey()));
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
        return this.mappedItems;
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
