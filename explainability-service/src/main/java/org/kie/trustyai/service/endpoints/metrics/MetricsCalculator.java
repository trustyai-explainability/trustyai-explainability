package org.kie.trustyai.service.endpoints.metrics;

import java.util.List;

import javax.inject.Singleton;

import org.kie.trustyai.explainability.metrics.FairnessMetrics;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.payloads.PayloadConverter;
import org.kie.trustyai.service.payloads.spd.GroupStatisticalParityDifferenceRequest;

@Singleton
public class MetricsCalculator {

    public double calculateSPD(Dataframe dataframe, GroupStatisticalParityDifferenceRequest request) {
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
    }

    public double calculateDIR(Dataframe dataframe, GroupStatisticalParityDifferenceRequest request) {
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
    }
}
