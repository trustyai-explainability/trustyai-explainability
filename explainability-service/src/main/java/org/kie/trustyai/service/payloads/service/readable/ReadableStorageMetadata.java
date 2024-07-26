package org.kie.trustyai.service.payloads.service.readable;

import org.kie.trustyai.service.data.metadata.StorageMetadata;

public class ReadableStorageMetadata {
    private ReadableSchema inputSchema;
    private ReadableSchema outputSchema;
    private String inputTensorName;
    private String outputTensorName;
    private int observations;

    private ReadableStorageMetadata() {
    }

    public ReadableSchema getInputSchema() {
        return inputSchema;
    }

    public ReadableSchema getOutputSchema() {
        return outputSchema;
    }

    public String getInputTensorName() {
        return inputTensorName;
    }

    public String getOutputTensorName() {
        return outputTensorName;
    }

    public int getObservations() {
        return observations;
    }

    public void setInputSchema(ReadableSchema inputSchema) {
        this.inputSchema = inputSchema;
    }

    public void setOutputSchema(ReadableSchema outputSchema) {
        this.outputSchema = outputSchema;
    }

    public void setInputTensorName(String inputTensorName) {
        this.inputTensorName = inputTensorName;
    }

    public void setOutputTensorName(String outputTensorName) {
        this.outputTensorName = outputTensorName;
    }

    public void setObservations(int observations) {
        this.observations = observations;
    }

    public static ReadableStorageMetadata from(StorageMetadata sm) {
        ReadableStorageMetadata rsm = new ReadableStorageMetadata();
        rsm.setInputSchema(new ReadableSchema(sm.getInputSchema()));
        rsm.setOutputSchema(new ReadableSchema(sm.getOutputSchema()));
        rsm.setInputTensorName(sm.getInputTensorName());
        rsm.setOutputTensorName(sm.getOutputTensorName());
        rsm.setObservations(sm.getObservations());
        return rsm;
    }
}