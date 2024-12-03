package org.kie.trustyai.service.data.utils;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.service.payloads.service.Schema;
import org.kie.trustyai.service.payloads.service.SchemaItem;
import org.kie.trustyai.service.payloads.values.DataType;

public class MetadataUtils {

    private MetadataUtils() {

    }

    public static SchemaItem extractRowSchema(Dataframe dataframe, int i) {
        final Value value = dataframe.getValue(0, i);

        final SchemaItem schemaItem = new SchemaItem();

        if (value.getUnderlyingObject() instanceof Integer) {
            schemaItem.setType(DataType.INT32);
        } else if (value.getUnderlyingObject() instanceof Double) {
            schemaItem.setType(DataType.DOUBLE);
        } else if (value.getUnderlyingObject() instanceof Float) {
            schemaItem.setType(DataType.FLOAT);
        } else if (value.getUnderlyingObject() instanceof Long) {
            schemaItem.setType(DataType.INT64);
        } else if (value.getUnderlyingObject() instanceof Boolean) {
            schemaItem.setType(DataType.BOOL);
        } else if (value.getUnderlyingObject() instanceof String) {
            schemaItem.setType(DataType.STRING);
        } else if (value.getUnderlyingObject() instanceof Map) {
            schemaItem.setType(DataType.MAP);
        } else if (value.getUnderlyingObject() instanceof Tensor<?>) {
            schemaItem.setType(DataType.TENSOR);
        }
        schemaItem.setName(dataframe.getColumnNames().get(i));
        schemaItem.setColumnIndex(i);

        // grab unique values
        return schemaItem;
    }

    public static Schema getInputSchema(Dataframe dataframe) {
        return Schema.from(dataframe
                .getInputsIndices()
                .stream()
                .map(i -> extractRowSchema(dataframe, i))
                .collect(Collectors.toMap(SchemaItem::getName, Function.identity())));
    }

    public static Schema getOutputSchema(Dataframe dataframe) {
        return Schema.from(dataframe.getOutputsIndices().stream()
                .map(i -> extractRowSchema(dataframe, i))
                .collect(Collectors.toMap(SchemaItem::getName, Function.identity())));
    }
}
