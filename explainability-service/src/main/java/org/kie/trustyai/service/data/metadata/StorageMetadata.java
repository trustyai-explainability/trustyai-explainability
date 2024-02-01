package org.kie.trustyai.service.data.metadata;

import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.payloads.service.Schema;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
public class StorageMetadata {
    private static final Logger LOG = Logger.getLogger(StorageMetadata.class);


    @OneToMany(cascade = CascadeType.ALL)
    // List of InputSchema, OutputSchema
    // needed for oneToX abstraction to work
    private List<Schema> schemas = Arrays.asList(new Schema(), new Schema());

    private int observations = 0;

    @Id
    private String modelId;

    public StorageMetadata() {
    }

    public Schema getOutputSchema() {
        return schemas.get(1);
    }

    public void setOutputSchema(Schema outputSchema) {
        this.schemas.set(1, outputSchema);
    }

    public Schema getInputSchema() {
        return schemas.get(0);
    }

    public void setInputSchema(Schema inputSchema) {
        this.schemas.set(0, inputSchema);
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
        if (otherSchema.equals(this.getInputSchema())) {
            for (String columnName : this.getInputSchema().getItems().keySet()) {
                // propagate nulls: null values mean there too many unique values to enumerate, therefore null + x = x + null = null
                if (this.getInputSchema().getItems().get(columnName).getColumnValues() == null || otherSchema.getItems().get(columnName).getColumnValues() == null) {
                    this.getInputSchema().getItems().get(columnName).setColumnValues(null);
                } else {
                    this.getInputSchema().getItems().get(columnName).getColumnValues().addAll(otherSchema.getItems().get(columnName).getColumnValues());
                }
            }
        } else {
            final String message = "Original schema and schema-to-merge are not compatible";
            throw new InvalidSchemaException(message);
        }
    }

    public void mergeOutputSchema(Schema otherSchema) {
        if (otherSchema.equals(this.getOutputSchema())) {
            for (String columnName : this.getOutputSchema().getItems().keySet()) {
                // propagate nulls: null values mean there too many unique values to enumerate, therefore null + x = x + null = null
                if (this.getOutputSchema().getItems().get(columnName).getColumnValues() == null || otherSchema.getItems().get(columnName).getColumnValues() == null) {
                    this.getOutputSchema().getItems().get(columnName).setColumnValues(null);
                } else {
                    this.getOutputSchema().getItems().get(columnName).getColumnValues().addAll(otherSchema.getItems().get(columnName).getColumnValues());
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
