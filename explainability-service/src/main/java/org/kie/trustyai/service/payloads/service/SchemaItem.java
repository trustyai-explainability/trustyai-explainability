package org.kie.trustyai.service.payloads.service;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.kie.trustyai.service.payloads.values.DataType;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class SchemaItem {
    private DataType type;
    private String name;

    @ElementCollection
    private Set<Object> values;
    private int index;
    @Id
    private UUID id;

    public SchemaItem() {
        this.id = UUID.randomUUID();
    }

    public SchemaItem(DataType type, String name, Set<Object> values, int index) {
        this.type = type;
        this.name = name;
        this.values = values;
        this.index = index;
        this.id = UUID.randomUUID();
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Object> getValues() {
        return values;
    }

    public void setValues(Set<Object> values) {
        this.values = values;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SchemaItem that = (SchemaItem) o;
        return index == that.index && type == that.type && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, index);
    }

    @Override
    public String toString() {
        return "SchemaItem{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", values=" + values +
                ", index=" + index +
                '}';
    }

    @Access(AccessType.FIELD)
    public UUID getId() {
        return id;
    }
}
