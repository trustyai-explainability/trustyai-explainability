package org.kie.trustyai.service.payloads.service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.UnderlyingObject;
import org.kie.trustyai.service.payloads.values.DataType;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
public class SchemaItem {
    private DataType type;
    private String name;

    @JsonAlias({ "values" })
    @ElementCollection
    private Set<UnderlyingObject> columnValues;

    @JsonAlias({ "index" })
    private int columnIndex;

    @Id
    @GeneratedValue
    long id;

    public SchemaItem() {
    }

    public SchemaItem(DataType type, String name, Set<UnderlyingObject> columnValues, int columnIndex) {
        this.type = type;
        this.name = name;
        this.columnValues = columnValues;
        this.columnIndex = columnIndex;
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

    public Set<UnderlyingObject> getColumnValues() {
        return columnValues;
    }

    // for compatibility with legacy metadata
    @JsonProperty("values")
    public void setColumnValuesFromLegacy(Set<Object> values) {
        this.columnValues =
                values.stream().map(v -> v instanceof UnderlyingObject ? (UnderlyingObject) v : new UnderlyingObject(v))
                        .collect(Collectors.toSet());
    }

    public void setColumnValues(Set<UnderlyingObject> values) {
        this.columnValues = values;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(int index) {
        this.columnIndex = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SchemaItem that = (SchemaItem) o;
        return columnIndex == that.columnIndex && type == that.type && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, columnIndex);
    }

    @Override
    public String toString() {
        return "SchemaItem{" +
                "type=" + type +
                ", name='" + name + '\'' +
                ", values=" + columnValues +
                ", index=" + columnIndex +
                '}';
    }
}
