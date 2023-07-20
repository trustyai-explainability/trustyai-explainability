package org.kie.trustyai.service.validators.metrics.identity;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IdentityMetricRequestValidator.class)
public @interface ValidIdentityMetricRequest {
    String message() default "The supplied metric request details are not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
