package org.kie.trustyai.service.endpoints.metrics.fairness.group.legacy;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.fairness.FairnessDefinitions;
import org.kie.trustyai.metrics.fairness.group.GroupStatisticalParityDifference;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.endpoints.metrics.fairness.group.GroupEndpoint;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Tag(name = "Statistical Parity Difference Endpoint (Legacy)", description = "Statistical Parity Difference (SPD) measures imbalances in classifications by calculating the " +
        "difference between the proportion of the majority and protected classes getting a particular outcome. This endpoint will be moving to /metrics/group/fairness/spd in a later release.")
@Path("/metrics/spd")
@Deprecated(forRemoval = true)
public class GroupStatisticalParityDifferenceEndpointLegacy extends GroupEndpoint {
    @Override
    public MetricThreshold thresholdFunction(Number delta, Number metricValue) {
        if (delta == null) {
            return new MetricThreshold(
                    metricsConfig.spd().thresholdLower(),
                    metricsConfig.spd().thresholdUpper(),
                    metricValue.doubleValue());
        } else {
            return new MetricThreshold(
                    0 - delta.doubleValue(),
                    delta.doubleValue(),
                    metricValue.doubleValue());
        }
    }

    @Override
    public String specificDefinitionFunction(String outcomeName, Value favorableOutcomeAttr, String protectedAttribute, String privileged, String unprivileged, Number metricValue) {
        return FairnessDefinitions.defineGroupStatisticalParityDifference(
                protectedAttribute,
                privileged,
                unprivileged,
                outcomeName,
                favorableOutcomeAttr,
                metricValue.doubleValue());
    };

    @Override
    @CacheResult(cacheName = "metrics-calculator", keyGenerator = MetricCalculationCacheKeyGen.class)
    public double calculate(Dataframe dataframe, @ValidReconciledMetricRequest GroupMetricRequest request) {
        LOG.debug("Cache miss. Calculating metric for " + request.getModelId());
        try {
            final int protectedIndex = dataframe.getColumnNames().indexOf(request.getProtectedAttribute());

            final Value privilegedAttr = PayloadConverter.convertToValue(request.getPrivilegedAttribute().getReconciledType().get());

            final Dataframe privileged = dataframe.filterByColumnValue(protectedIndex,
                    value -> value.equals(privilegedAttr));
            final Value unprivilegedAttr = PayloadConverter.convertToValue(request.getUnprivilegedAttribute().getReconciledType().get());
            final Dataframe unprivileged = dataframe.filterByColumnValue(protectedIndex,
                    value -> value.equals(unprivilegedAttr));
            final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome().getReconciledType().get());
            final Type favorableOutcomeAttrType = PayloadConverter.convertToType(request.getFavorableOutcome().getReconciledType().get().getType());
            return GroupStatisticalParityDifference.calculate(privileged, unprivileged,
                    List.of(new Output(request.getOutcomeName(), favorableOutcomeAttrType, favorableOutcomeAttr, 1.0)));
        } catch (Exception e) {
            throw new MetricCalculationException(e.getMessage(), e);
        }
    }

    @Override
    public String getGeneralDefinition() {
        return FairnessDefinitions.defineGroupStatisticalParityDifference();
    }

    public GroupStatisticalParityDifferenceEndpointLegacy() {
        super("SPD");
    }

}
