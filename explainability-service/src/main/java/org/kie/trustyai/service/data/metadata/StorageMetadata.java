package org.kie.trustyai.service.data.metadata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.dataframe.DataframeMetadata;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.payloads.service.Schema;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

@Entity
public class StorageMetadata {
    private static final Logger LOG = Logger.getLogger(StorageMetadata.class);

    @OneToMany(cascade = CascadeType.ALL)
    // List of InputSchema, OutputSchema
    // needed for oneToX abstraction to work
    private List<Schema> schemas = Arrays.asList(new Schema(), new Schema());

    private String inputTensorName = DataframeMetadata.DEFAULT_INPUT_TENSOR_NAME;
    private String outputTensorName = DataframeMetadata.DEFAULT_OUTPUT_TENSOR_NAME;

    private int observations = 0;
    private boolean recordedInferences = false;

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

    public boolean isRecordedInferences() {
        return recordedInferences;
    }

    public void setRecordedInferences(boolean recordedInferences) {
        this.recordedInferences = recordedInferences;
    }

    public void mergeInputSchema(Schema otherSchema) {
        if (otherSchema.equals(this.getInputSchema())) {
            // pass
        } else {
            final String message = "Original schema and schema-to-merge are not compatible";
            throw new InvalidSchemaException(message);
        }
    }

    public void mergeOutputSchema(Schema otherSchema) {
        if (otherSchema.equals(this.getOutputSchema())) {
            // pass
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

    public String getInputTensorName() {
        return inputTensorName;
    }

    public void setInputTensorName(String inputTensorName) {
        this.inputTensorName = inputTensorName;
    }

    public String getOutputTensorName() {
        return outputTensorName;
    }

    public void setOutputTensorName(String outputTensorName) {
        this.outputTensorName = outputTensorName;
    }

    public Map<String, String> getJointNameAliases() {
        HashMap<String, String> jointMapping = new HashMap<>();
        jointMapping.putAll(getInputSchema().getNameMapping());
        jointMapping.putAll(getOutputSchema().getNameMapping());
        return jointMapping;
    }
}
