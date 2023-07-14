package org.kie.trustyai.service.validators.metrics;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MetricReconciliationValidator.class)
public @interface ValidReconciledMetricRequest {
    String message() default "The supplied metric has not been validated.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
