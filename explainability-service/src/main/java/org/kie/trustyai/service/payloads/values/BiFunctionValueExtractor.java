package org.kie.trustyai.service.payloads.values;

import java.util.function.BiFunction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.valueextraction.ExtractedValue;
import jakarta.validation.valueextraction.ValueExtractor;

// needed to exempt the BiFunctions in the Metric Directory from Javax container validation
@ApplicationScoped
public class BiFunctionValueExtractor implements ValueExtractor<BiFunction<?, @ExtractedValue ?, ?>> {
    @Override
    public void extractValues(BiFunction<?, @ExtractedValue ?, ?> originalValue, ValueReceiver receiver) {
        receiver.value("BiFunction", originalValue);
    }
}
