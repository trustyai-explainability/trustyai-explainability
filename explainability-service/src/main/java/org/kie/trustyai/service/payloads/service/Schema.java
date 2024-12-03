package org.kie.trustyai.service.payloads.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.hibernate.collection.spi.AbstractPersistentCollection;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "StorageSchema")
@JsonIgnoreProperties(value = { "remapCount" }, allowGetters = true)
public class Schema {
    private static final Logger LOG = Logger.getLogger(Schema.class);

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "storageSchema_originalItems")
    // a map of the original column names keying their corresponding schemaItems
    private Map<String, SchemaItem> items;

    @ElementCollection
    private Map<String, String> nameMapping = new HashMap<>();

    @OneToMany(cascade = CascadeType.ALL)
    @JoinTable(name = "storageSchema_mappedItems")
    // a constructed map containing any aliased columns, keying their schemaItems
    private Map<String, SchemaItem> mappedItems = new HashMap<>();

    @Id
    @GeneratedValue
    private Long id;

    public Schema() {
        this.items = new ConcurrentHashMap<>();
        calculateNameMappedItems(); // trivial state: just sets mappedItems == items
    }

    public Schema(Map<String, SchemaItem> items) {
        this.items = items;
        calculateNameMappedItems(); // trivial state: just sets mappedItems == items
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

    // getters and setters =============================================================================================
    public Map<String, String> getNameMapping() {
        return nameMapping;
    }

    public Map<String, SchemaItem> getNameMappedItems() {
        return this.mappedItems;
    }

    public Set<String> getNameMappedKeySet() {
        return new HashSet<>(this.mappedItems.keySet());
    }

    // for specific usage for Hibernate initialization
    public void setNameMapping(Map<String, String> nameMapping) {
        this.nameMapping = nameMapping;
        calculateNameMappedItems();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
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

    /*
     * Get a verbose description of why two schemas are not equal. Returns Optional.empty() if the schemas are equal.
     */
    public Optional<String> equalsVerbose(Object o) {
        if (this == o)
            return Optional.empty();
        if (null == o) {
            return Optional.of("Compared schema is null");
        }
        if (getClass() != o.getClass()) {
            return Optional.of("Compared schema is not a schema object, received: " + o.getClass());
        }
        Schema schema = (Schema) o;

        if (null == schema.items) {
            return Optional.of("Compared schema.items is null");
        }

        if (null == items) {
            return Optional.of("Existing schema.items is null");
        }

        // unwrap proxy sets  if necessary
        Set keys;
        if (AbstractPersistentCollection.class == items.keySet().getClass().getEnclosingClass()) {
            keys = items.keySet().stream().collect(Collectors.toSet());
        } else {
            keys = items.keySet();
        }

        Set otherKeys;
        if (AbstractPersistentCollection.class == schema.items.keySet().getClass().getEnclosingClass()) {
            otherKeys = schema.items.keySet().stream().collect(Collectors.toSet());
        } else {
            otherKeys = schema.items.keySet();
        }

        if (!keys.equals(otherKeys)) {
            return Optional.of(String.format("Schema column names do not match. Existing schema columns=%s, comparison schema columns=%s", keys, otherKeys));
        }

        List<String> mismatches = new ArrayList<>();
        for (Map.Entry<String, SchemaItem> item : items.entrySet()) {
            SchemaItem thisSchemaItem = item.getValue();
            SchemaItem thatSchemaItem = schema.items.get(item.getKey());
            if (!thisSchemaItem.equals(thatSchemaItem)) {
                mismatches.add("Original SchemaItem=" + thisSchemaItem + " does not match new SchemaItem=" + thatSchemaItem);
            }
        }
        if (mismatches.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(String.join(", ", mismatches));
        }
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
