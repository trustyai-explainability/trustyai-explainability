package org.kie.trustyai.service.payloads.service;

public class SchemaItem {
    public String type;
    public String name;

    public SchemaItem() {

    }

    public SchemaItem(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
