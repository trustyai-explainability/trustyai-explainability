package org.kie.trustyai.service.validators.metrics;

import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.fairness.group.GroupMetricRequest;
import org.kie.trustyai.service.payloads.values.ReconcilableFeature;
import org.kie.trustyai.service.payloads.values.ReconcilableOutput;
import org.kie.trustyai.service.payloads.values.ReconcilerMatcher;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.lang.reflect.Field;

@ApplicationScoped
public class MetricReconciliationValidator implements ConstraintValidator<ValidReconciledMetricRequest, BaseMetricRequest> {

    @Override
    public void initialize(ValidReconciledMetricRequest constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(BaseMetricRequest request, ConstraintValidatorContext context) {
        for (Field f : request.getClass().getDeclaredFields()) {
            if (f.getType().isAssignableFrom(ReconcilableFeature.class) && f.isAnnotationPresent(ReconcilerMatcher.class)) {
                try {
                    ReconcilableFeature fieldValue = (ReconcilableFeature) f.get(request);
                    if (fieldValue.getTypeToReconcile().isEmpty()) {
                        return false;
                    }
                } catch (IllegalAccessException e){
                    return false;
                }
            } else if (f.getType().isAssignableFrom(ReconcilableOutput.class) && f.isAnnotationPresent(ReconcilerMatcher.class)) {
                try {
                    ReconcilableOutput fieldValue = (ReconcilableOutput) f.get(request);
                    if (fieldValue.getTypeToReconcile().isEmpty()) {
                        return false;
                    }
                } catch (IllegalAccessException e){
                    return false;
                }
            }
        }
        return true;
    }
}
