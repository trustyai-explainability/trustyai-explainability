package org.kie.trustyai.service.validators.metrics.fairness.group;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AdvancedGroupMetricRequestValidator.class)
public @interface ValidAdvancedGroupMetricRequest {
    String message() default "The supplied metric request details are not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
