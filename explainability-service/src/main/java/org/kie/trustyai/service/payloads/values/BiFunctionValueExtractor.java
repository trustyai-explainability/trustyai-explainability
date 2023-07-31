package org.kie.trustyai.service.payloads.values;

import java.util.function.BiFunction;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.valueextraction.ExtractedValue;
import javax.validation.valueextraction.ValueExtractor;

// needed to exempt the BiFunctions in the Metric Directory from Javax container validation
@ApplicationScoped
public class BiFunctionValueExtractor implements ValueExtractor<BiFunction<?, @ExtractedValue ?, ?>> {
    @Override
    public void extractValues(BiFunction<?, @ExtractedValue ?, ?> originalValue, ValueReceiver receiver) {
        receiver.value("BiFunction", originalValue);
    }
}
