package org.kie.trustyai.service.payloads.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class Schema {
    private final Map<String, SchemaItem> items;
    private Map<String, String> nameMapping = new HashMap<>();

    public Schema() {
        this.items = new ConcurrentHashMap<>();
    }

    public Schema(Map<String, SchemaItem> items) {
        this.items = items;
    }

    public Schema(Map<String, SchemaItem> items, Map<String, String> nameMapping) {
        this.items = items;
        this.nameMapping = nameMapping;
    }

    public static Schema from(Map<String, SchemaItem> items) {
        return new Schema(items);
    }

    public Map<String, SchemaItem> getItems() {
        return items;
    }

    //@CacheResult(cacheName = "schema-name-mapped-items", keyGenerator = SchemaNameMappingCacheKeyGen.class)
    public Map<String, SchemaItem> retrieveNameMappedItems() {
        Map<String, SchemaItem> returnMap = new HashMap<>();
        for (Map.Entry<String, SchemaItem> entry : items.entrySet()){
            if (nameMapping.containsKey(entry.getKey())){
                returnMap.put(nameMapping.get(entry.getKey()), entry.getValue());
            } else {
                returnMap.put(entry.getKey(), entry.getValue());
            }
        }
        return returnMap;
    }

    public Map<String, String> getNameMapping() {
        return nameMapping;
    }

    public void setNameMapping(Map<String, String> nameMapping) {
        this.nameMapping = nameMapping;
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
}
