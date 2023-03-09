package org.kie.trustyai.service.endpoints.metrics;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.metrics.FairnessMetrics;
import org.kie.trustyai.explainability.metrics.utils.FairnessDefinitions;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.payloads.BaseMetricRequest;
import org.kie.trustyai.service.payloads.PayloadConverter;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
public class MetricsCalculator {

    private static final Logger LOG = Logger.getLogger(MetricsCalculator.class);

    @CacheResult(cacheName = "metrics-calculator", keyGenerator = MetricCalculationCacheKeyGen.class)
    public double calculateSPD(Dataframe dataframe, BaseMetricRequest request) throws MetricCalculationException {
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
            return FairnessMetrics.groupStatisticalParityDifference(privileged, unprivileged,
                    List.of(new Output(request.getOutcomeName(), favorableOutcomeAttrType, favorableOutcomeAttr, 1.0)));
        } catch (Exception e) {
            throw new MetricCalculationException(e.getMessage(), e);
        }
    }

    public String getSPDDefinition(double spd, BaseMetricRequest request) {
        final String outcomeName = request.getOutcomeName();
        final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
        final String protectedAttribute = request.getProtectedAttribute();
        final String priviliged = PayloadConverter.convertToValue(request.getPrivilegedAttribute()).toString();
        final String unpriviliged = PayloadConverter.convertToValue(request.getUnprivilegedAttribute()).toString();

        return FairnessDefinitions.defineGroupStatisticalParityDifference(
                protectedAttribute,
                priviliged,
                unpriviliged,
                outcomeName,
                favorableOutcomeAttr,
                spd);
    }

    @CacheResult(cacheName = "metrics-calculator", keyGenerator = MetricCalculationCacheKeyGen.class)
    public double calculateDIR(Dataframe dataframe, BaseMetricRequest request) {
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
            return FairnessMetrics.groupDisparateImpactRatio(privileged, unprivileged,
                    List.of(new Output(request.getOutcomeName(), favorableOutcomeAttrType, favorableOutcomeAttr, 1.0)));
        } catch (Exception e) {
            throw new MetricCalculationException(e.getMessage(), e);
        }
    }

    public String getDIRDefinition(double dir, BaseMetricRequest request) {
        final String outcomeName = request.getOutcomeName();
        final Value favorableOutcomeAttr = PayloadConverter.convertToValue(request.getFavorableOutcome());
        final String protectedAttribute = request.getProtectedAttribute();
        final String priviliged = PayloadConverter.convertToValue(request.getPrivilegedAttribute()).toString();
        final String unpriviliged = PayloadConverter.convertToValue(request.getUnprivilegedAttribute()).toString();
        return FairnessDefinitions.defineGroupDisparateImpactRatio(
                protectedAttribute,
                priviliged,
                unpriviliged,
                outcomeName,
                favorableOutcomeAttr,
                dir);
    }
}
