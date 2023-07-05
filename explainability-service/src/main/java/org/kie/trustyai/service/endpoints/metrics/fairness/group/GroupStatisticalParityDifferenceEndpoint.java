package org.kie.trustyai.service.endpoints.metrics.fairness.group;

import java.util.List;
import javax.ws.rs.*;

import io.quarkus.cache.CacheResult;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.metrics.fairness.FairnessDefinitions;
import org.kie.trustyai.metrics.fairness.group.GroupStatisticalParityDifference;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.fairness.group.ReconciledGroupMetricRequest;

@Tag(name = "Statistical Parity Difference Endpoint", description = "Statistical Parity Difference (SPD) measures imbalances in classifications by calculating the " +
        "difference between the proportion of the majority and protected classes getting a particular outcome.")
@Path("/metrics/spd")
public class GroupStatisticalParityDifferenceEndpoint extends GroupEndpoint {

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
    public Number calculate(Dataframe dataframe, ReconciledGroupMetricRequest request) {
        LOG.debug("Cache miss. Calculating metric for " + request.getModelId());
        try {
            final int protectedIndex = dataframe.getColumnNames().indexOf(request.getProtectedAttribute());

            final Value privilegedAttr = PayloadConverter.convertToValue(request.getPrivilegedAttribute());

            final Dataframe privileged = dataframe.filterByColumnValue(protectedIndex,
                    value -> value.equals(privilegedAttr));
            final Value unprivilegedAttr = PayloadConverter.convertToValue(request.getUnprivilegedAttribute());
            final Dataframe unprivileged = dataframe.filterByColumnValue(protectedIndex,
                    value -> value.equals(unprivilegedAttr));
            final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
            final Type favorableOutcomeAttrType = PayloadConverter.convertToType(request.getFavorableOutcome().getType());
            return GroupStatisticalParityDifference.calculate(privileged, unprivileged,
                    List.of(new Output(request.getOutcomeName(), favorableOutcomeAttrType, favorableOutcomeAttr, 1.0)));
        } catch (Exception e) {
            throw new MetricCalculationException(e.getMessage(), e);
        }
    }

    @Override
    public String getGeneralDefinition(){
        return FairnessDefinitions.defineGroupStatisticalParityDifference();
    }

    public GroupStatisticalParityDifferenceEndpoint(){
        super("SPD");
    }

}
