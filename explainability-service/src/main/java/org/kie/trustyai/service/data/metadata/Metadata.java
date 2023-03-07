package org.kie.trustyai.service.data.metadata;

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

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
}
