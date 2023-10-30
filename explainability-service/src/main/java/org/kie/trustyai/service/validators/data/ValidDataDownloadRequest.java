package org.kie.trustyai.service.validators.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ ElementType.TYPE_USE, ElementType.PARAMETER, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DataDownloadRequestValidator.class)
public @interface ValidDataDownloadRequest {
    String message() default "The supplied data download request has not been validated.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
