package org.kie.trustyai.service.payloads.service.readable;

import org.kie.trustyai.service.payloads.service.SchemaItem;
import org.kie.trustyai.service.payloads.values.DataType;

public class ReadableSchemaItem {
    private DataType type;
    private String name;
    private int columnIndex;

    public ReadableSchemaItem() {
    }

    public ReadableSchemaItem(SchemaItem schemaItem) {
        type = schemaItem.getType();
        name = schemaItem.getName();
        columnIndex = schemaItem.getColumnIndex();
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

    public void setColumnIndex(int columnIndex) {
        this.columnIndex = columnIndex;
    }
}
