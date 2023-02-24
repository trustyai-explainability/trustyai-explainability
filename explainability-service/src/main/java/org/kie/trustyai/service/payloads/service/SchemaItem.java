package org.kie.trustyai.service.payloads.service;

import org.kie.trustyai.service.payloads.values.DataType;

public class SchemaItem {
    private DataType type;
    private String name;
    private int index;

    public SchemaItem() {

    }

    public SchemaItem(DataType type, String name, int index) {
        this.type = type;
        this.name = name;
        this.index = index;
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

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
