package org.kie.trustyai.service.payloads.metrics.fairness.group;

import javax.enterprise.inject.Instance;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.payloads.metrics.ReconciledBaseMetricRequest;
import org.kie.trustyai.service.payloads.values.DataType;
import org.kie.trustyai.service.payloads.values.TypedValue;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashMap;
import java.util.Map;

@JsonPropertyOrder({ "protected", "favorable" })
public class ReconciledGroupMetricRequest extends ReconciledBaseMetricRequest {
    private final String protectedAttribute;
    private final String outcomeName;
    private final Double thresholdDelta;

    private Integer batchSize;

    private final TypedValue privilegedAttribute;
    private final TypedValue favorableOutcome;
    private final TypedValue unprivilegedAttribute;


    public ReconciledGroupMetricRequest(String protectedAttribute, String outcomeName, String modelId, String requestName, String metricName, Double thresholdDelta, Integer batchSize,
                                        TypedValue privilegedAttribute, TypedValue favorableOutcome, TypedValue unprivilegedAttribute) {
        super(modelId, requestName, metricName);
        this.protectedAttribute = protectedAttribute;
        this.outcomeName = outcomeName;
        this.thresholdDelta = thresholdDelta;
        this.batchSize = batchSize;
        this.privilegedAttribute = privilegedAttribute;
        this.favorableOutcome = favorableOutcome;
        this.unprivilegedAttribute = unprivilegedAttribute;
    }

    public ReconciledGroupMetricRequest(GroupMetricRequest request, DataType protectedType, DataType outcomeType) {

        super(request.getModelId(), request.getRequestName(), request.getMetricName());
        protectedAttribute = request.getProtectedAttribute();
        outcomeName = request.getOutcomeName();
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

        super.setTags(retrieveTags());

    }

    public Map<String, String> retrieveTags(){
        Map<String, String> tags = new HashMap<>();
        tags.put("outcome", this.getOutcomeName());
        tags.put("favorable_value", this.getFavorableOutcome().toString());
        tags.put("protected", this.getProtectedAttribute());
        tags.put("privileged", this.getPrivilegedAttribute().toString());
        tags.put("unprivileged", this.getUnprivilegedAttribute().toString());
        tags.put("batch_size", String.valueOf(this.getBatchSize()));
        return tags;
    }

    public static ReconciledGroupMetricRequest reconcile(GroupMetricRequest request, Instance<DataSource> dataSource) {
        final Metadata metadata = dataSource.get().getMetadata(request.getModelId());
        return new ReconciledGroupMetricRequest(
                request,
                metadata.getInputSchema().getItems().get(request.getProtectedAttribute()).getType(),
                metadata.getOutputSchema().getItems().get(request.getOutcomeName()).getType());
    }

    public static ReconciledGroupMetricRequest reconcile(GroupMetricRequest request, Metadata metadata) {
        return new ReconciledGroupMetricRequest(
                request,
                metadata.getInputSchema().getItems().get(request.getProtectedAttribute()).getType(),
                metadata.getOutputSchema().getItems().get(request.getOutcomeName()).getType());
    }

    public String getProtectedAttribute() {
        return protectedAttribute;
    }

    public String getOutcomeName() {
        return outcomeName;
    }

    public Double getThresholdDelta() {
        return thresholdDelta;
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

    public TypedValue getFavorableOutcome() {
        return favorableOutcome;
    }


    public TypedValue getUnprivilegedAttribute() {
        return unprivilegedAttribute;
    }

}
