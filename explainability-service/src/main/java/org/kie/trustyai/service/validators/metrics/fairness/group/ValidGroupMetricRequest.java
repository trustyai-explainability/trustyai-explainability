package org.kie.trustyai.service.validators.metrics.fairness.group;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Target({ ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GroupMetricRequestValidator.class)
public @interface ValidGroupMetricRequest {
    String message() default "The supplied metric request details are not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
