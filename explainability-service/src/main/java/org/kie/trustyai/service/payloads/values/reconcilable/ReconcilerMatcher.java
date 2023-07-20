package org.kie.trustyai.service.payloads.values.reconcilable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.kie.trustyai.service.payloads.values.reconcilable.deserializers.ReconcilableFieldDeserializer;
import org.kie.trustyai.service.payloads.values.reconcilable.serializers.ReconcilableFieldSerializer;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

// use this annotation to describe a metric request field that must align with a specific type in the service metadata
// the nameProvider is the class.getX() function that provides the *name* of the request field.
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonDeserialize(using = ReconcilableFieldDeserializer.class)
@JsonSerialize(using = ReconcilableFieldSerializer.class)
public @interface ReconcilerMatcher {
    String nameProvider();
}
