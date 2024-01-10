package org.kie.trustyai.service.validators.serviceRequests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NameMappingRequestValidator.class)
public @interface ValidNameMappingRequest {
    String message() default "The supplied name mapping is not valid.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
