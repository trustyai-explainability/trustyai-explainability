package org.kie.trustyai.service.payloads;

import javax.enterprise.inject.Instance;

import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "protected", "favorable" })
public class ReconciledMetricRequest {
    private String protectedAttribute;
    private String outcomeName;
    private String modelId;

    // this is the unique name of this specific request
    private String requestName;

    // this is the name of the metric that this request calculates, e.g., DIR or SPD
    private String metricName;
    private Double thresholdDelta;

    private Integer batchSize;

    private TypedValue privilegedAttribute;
    private TypedValue favorableOutcome;
    private TypedValue unprivilegedAttribute;

    public ReconciledMetricRequest() {
    };

    public ReconciledMetricRequest(String protectedAttribute, String outcomeName, String modelId, String requestName, String metricName, Double thresholdDelta, Integer batchSize,
            TypedValue privilegedAttribute, TypedValue favorableOutcome, TypedValue unprivilegedAttribute) {
        this.protectedAttribute = protectedAttribute;
        this.outcomeName = outcomeName;
        this.modelId = modelId;
        this.requestName = requestName;
        this.metricName = metricName;
        this.thresholdDelta = thresholdDelta;
        this.batchSize = batchSize;
        this.privilegedAttribute = privilegedAttribute;
        this.favorableOutcome = favorableOutcome;
        this.unprivilegedAttribute = unprivilegedAttribute;
    }

    public ReconciledMetricRequest(BaseMetricRequest request, DataType protectedType, DataType outcomeType) {

        protectedAttribute = request.getProtectedAttribute();
        outcomeName = request.getOutcomeName();
        modelId = request.getModelId();
        requestName = request.getRequestName();
        metricName = request.getMetricName();
        thresholdDelta = request.getThresholdDelta();
        batchSize = request.getBatchSize();

        this.privilegedAttribute = new TypedValue();
        this.privilegedAttribute.setType(protectedType);
        this.privilegedAttribute.setValue(request.getPrivilegedAttribute());

        this.unprivilegedAttribute = new TypedValue();
        this.unprivilegedAttribute.setType(protectedType);
        this.unprivilegedAttribute.setValue(request.getUnprivilegedAttribute());

        this.favorableOutcome = new TypedValue();
        this.favorableOutcome.setType(outcomeType);
        this.favorableOutcome.setValue(request.getFavorableOutcome());
    }

    public static ReconciledMetricRequest reconcile(BaseMetricRequest request, Instance<DataSource> dataSource) {
        final Metadata metadata = dataSource.get().getMetadata(request.getModelId());
        return new ReconciledMetricRequest(
                request,
                metadata.getInputSchema().getItems().get(request.getProtectedAttribute()).getType(),
                metadata.getOutputSchema().getItems().get(request.getOutcomeName()).getType());
    }

    public static ReconciledMetricRequest reconcile(BaseMetricRequest request, Metadata metadata) {
        return new ReconciledMetricRequest(
                request,
                metadata.getInputSchema().getItems().get(request.getProtectedAttribute()).getType(),
                metadata.getOutputSchema().getItems().get(request.getOutcomeName()).getType());
    }

    public String getProtectedAttribute() {
        return protectedAttribute;
    }

    public void setProtectedAttribute(String protectedAttribute) {
        this.protectedAttribute = protectedAttribute;
    }

    public String getOutcomeName() {
        return outcomeName;
    }

    public void setOutcomeName(String outcomeName) {
        this.outcomeName = outcomeName;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public String getRequestName() {
        return requestName;
    }

    public void setRequestName(String requestName) {
        this.requestName = requestName;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
    }

    public void setThresholdDelta(Double thresholdDelta) {
        this.thresholdDelta = thresholdDelta;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public TypedValue getPrivilegedAttribute() {
        return privilegedAttribute;
    }

    public void setPrivilegedAttribute(TypedValue privilegedAttribute) {
        this.privilegedAttribute = privilegedAttribute;
    }

    public TypedValue getFavorableOutcome() {
        return favorableOutcome;
    }

    public void setFavorableOutcome(TypedValue favorableOutcome) {
        this.favorableOutcome = favorableOutcome;
    }

    public TypedValue getUnprivilegedAttribute() {
        return unprivilegedAttribute;
    }

    public void setUnprivilegedAttribute(TypedValue unprivilegedAttribute) {
        this.unprivilegedAttribute = unprivilegedAttribute;
    }
}
