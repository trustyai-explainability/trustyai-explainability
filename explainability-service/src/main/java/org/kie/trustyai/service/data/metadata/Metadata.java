package org.kie.trustyai.service.data.metadata;

import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.service.payloads.service.SchemaItem;

public class Metadata {

    private List<SchemaItem> inputSchema = new ArrayList<>();
    private List<SchemaItem> outputSchema = new ArrayList<>();

    private int observations = 0;

    private String modelId;

    public Metadata() {

    }

    public List<SchemaItem> getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(List<SchemaItem> outputSchema) {
        this.outputSchema = outputSchema;
    }

    public List<SchemaItem> getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(List<SchemaItem> inputSchema) {
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

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
