package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.metrics.fairness.FairnessDefinitions;
import org.kie.trustyai.metrics.fairness.group.DisparateImpactRatio;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.fairness.group.ValidGroupMetricRequest;

import io.quarkus.cache.CacheResult;
import io.quarkus.resteasy.reactive.server.EndpointDisabled;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Path;

@ApplicationScoped
@Tag(name = "Fairness Metrics: Group: Disparate Impact Ratio", description = "Disparate Impact Ratio (DIR) measures imbalances in " +
        "classifications by calculating the ratio between the proportion of the majority and protected classes getting" +
        " a particular outcome.")
@EndpointDisabled(name = "endpoints.fairness", stringValue = "disable")
@Path("/metrics/group/fairness/dir")
public class DisparateImpactRatioEndpoint extends GroupEndpoint {
    public DisparateImpactRatioEndpoint() {
        super("DIR");
    }

    @Override
    public MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue) {
        if (delta == null) {
            return new MetricThreshold(
                    metricsConfig.dir().thresholdLower(),
                    metricsConfig.dir().thresholdUpper(),
                    metricValue.getValue());
        } else {
            return new MetricThreshold(
                    1 - delta.doubleValue(),
                    1 + delta.doubleValue(),
                    metricValue.getValue());
        }
    }

    @Override
    public String specificDefinitionFunction(String outcomeName, List<Value> favorableOutcomeAttr, String protectedAttribute, List<String> privileged, List<String> unprivileged,
            MetricValueCarrier metricValue) {
        return FairnessDefinitions.defineGroupDisparateImpactRatio(
                protectedAttribute,
                privileged,
                unprivileged,
                outcomeName,
                favorableOutcomeAttr,
                metricValue.getValue());
    }

    @Override
    @CacheResult(cacheName = "metrics-calculator-dir", keyGenerator = MetricCalculationCacheKeyGen.class)
    public MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request) {
        LOG.debug("Cache miss. Calculating metric for " + request.getModelId());
        @ValidGroupMetricRequest
        GroupMetricRequest gmRequest = (GroupMetricRequest) request;
        try {
            final int protectedIndex = dataframe.getColumnNames().indexOf(gmRequest.getProtectedAttribute());

            final List<Value> privilegedAttrs = PayloadConverter.convertToValues(gmRequest.getPrivilegedAttribute().getReconciledType().get());

            final Dataframe privileged = dataframe.filterByColumnValue(protectedIndex,
                    value -> privilegedAttrs.stream().anyMatch(value::equals));
            final List<Value> unprivilegedAttrs = PayloadConverter.convertToValues(gmRequest.getUnprivilegedAttribute().getReconciledType().get());
            final Dataframe unprivileged = dataframe.filterByColumnValue(protectedIndex,
                    value -> unprivilegedAttrs.stream().anyMatch(value::equals));
            final List<Value> favorableOutcomeAttrs = PayloadConverter.convertToValues(gmRequest.getFavorableOutcome().getReconciledType().get());
            final Type favorableOutcomeAttrType = PayloadConverter.convertToType(gmRequest.getFavorableOutcome().getReconciledType().get().get(0).getType());
            return new MetricValueCarrier(
                    DisparateImpactRatio.calculate(
                            privileged,
                            unprivileged,
                            favorableOutcomeAttrs.stream().map(v -> new Output(gmRequest.getOutcomeName(), favorableOutcomeAttrType, v, 1.0)).collect(Collectors.toList())));
        } catch (Exception e) {
            throw new MetricCalculationException(e.getMessage(), e);
        }
    }

    @Override
    public String getGeneralDefinition() {
        return FairnessDefinitions.defineGroupDisparateImpactRatio();
    }
}
