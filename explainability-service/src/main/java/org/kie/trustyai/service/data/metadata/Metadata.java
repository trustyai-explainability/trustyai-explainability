package org.kie.trustyai.service.data.metadata;

import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.payloads.service.Schema;

public class Metadata {

    private Schema inputSchema = new Schema();
    private Schema outputSchema = new Schema();

    private int observations = 0;

    private String modelId;

    public Metadata() {
    }

    public Schema getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(Schema outputSchema) {
        this.outputSchema = outputSchema;
    }

    public Schema getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(Schema inputSchema) {
        this.inputSchema = inputSchema;
    }

    public int getObservations() {
        return observations;
    }

    public void setObservations(int observations) {
        this.observations = observations;
    }

    public void incrementObservations(int observations) {
        this.observations += observations;
    }

    public void mergeInputSchema(Schema otherSchema) {
        if (otherSchema.equals(this.inputSchema)) {
            for (String columnName : this.inputSchema.getItems().keySet()) {
                // propagate nulls: null values mean there too many unique values to enumerate, therefore null + x = x + null = null
                if (this.inputSchema.getItems().get(columnName).getValues() == null || otherSchema.getItems().get(columnName).getValues() == null) {
                    this.inputSchema.getItems().get(columnName).setValues(null);
                } else {
                    this.inputSchema.getItems().get(columnName).getValues().addAll(otherSchema.getItems().get(columnName).getValues());
                }
            }
        } else {
            final String message = "Original schema and schema-to-merge are not compatible";
            throw new InvalidSchemaException(message);
        }
    }

    public void mergeOutputSchema(Schema otherSchema) {
        if (otherSchema.equals(this.outputSchema)) {
            for (String columnName : this.outputSchema.getItems().keySet()) {
                // propagate nulls: null values mean there too many unique values to enumerate, therefore null + x = x + null = null
                if (this.outputSchema.getItems().get(columnName).getValues() == null || otherSchema.getItems().get(columnName).getValues() == null) {
                    this.outputSchema.getItems().get(columnName).setValues(null);
                } else {
                    this.outputSchema.getItems().get(columnName).getValues().addAll(otherSchema.getItems().get(columnName).getValues());
                }
            }
        } else {
            final String message = "Original schema and schema-to-merge are not compatible";
            throw new InvalidSchemaException(message);
        }
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
