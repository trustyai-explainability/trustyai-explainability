package org.kie.trustyai.service.payloads.service;

import java.util.Objects;

import org.kie.trustyai.service.payloads.values.DataType;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchemaItem {
    private DataType type;
    private String name;

    @JsonAlias({ "index" })
    private int columnIndex;

    @Id
    @GeneratedValue
    long id;

    public SchemaItem() {
    }

    public SchemaItem(DataType type, String name, int columnIndex) {
        this.type = type;
        this.name = name;
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
                ", index=" + columnIndex +
                '}';
    }
}
