package org.kie.trustyai.service.data.metadata;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.payloads.service.Schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Metadata {
    private static final Logger LOG = Logger.getLogger(Metadata.class);

    @AttributeOverride(name = "items", column = @Column(name = "inputSchema_items"))
    private Schema inputSchema = new Schema();

    @AttributeOverride(name = "items", column = @Column(name = "outputSchema_items"))
    private Schema outputSchema = new Schema();

    private int observations = 0;

    @Id
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
                if (this.inputSchema.getItems().get(columnName).getColumnValues() == null || otherSchema.getItems().get(columnName).getColumnValues() == null) {
                    this.inputSchema.getItems().get(columnName).setColumnValues(null);
                } else {
                    this.inputSchema.getItems().get(columnName).getColumnValues().addAll(otherSchema.getItems().get(columnName).getColumnValues());
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
                if (this.outputSchema.getItems().get(columnName).getColumnValues() == null || otherSchema.getItems().get(columnName).getColumnValues() == null) {
                    this.outputSchema.getItems().get(columnName).setColumnValues(null);
                } else {
                    this.outputSchema.getItems().get(columnName).getColumnValues().addAll(otherSchema.getItems().get(columnName).getColumnValues());
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
