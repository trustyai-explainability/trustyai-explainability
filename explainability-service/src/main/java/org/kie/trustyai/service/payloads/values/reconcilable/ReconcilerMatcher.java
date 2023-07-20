package org.kie.trustyai.service.payloads.values.reconcilable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;

// use this annotation to describe a metric request field that must align with a specific type in the service metadata
// the nameProvider is the class.getX() function that provides the *name* of the request field.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
public @interface ReconcilerMatcher {
    String nameProvider();
}
