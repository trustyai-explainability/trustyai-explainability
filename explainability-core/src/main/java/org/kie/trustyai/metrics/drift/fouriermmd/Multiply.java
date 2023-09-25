package org.kie.trustyai.metrics.drift.fouriermmd;

import java.util.function.Function;

import org.kie.trustyai.explainability.model.Value;

/*
 * Accessory Function for transformColumn() calls.
 */

public class Multiply implements Function<Value, Value> {

    private double multiplier;

    public Multiply(double multiplier) {
        this.multiplier = multiplier;
    }

    public Value apply(Value value) {

        final double orig = value.asNumber();

        assert !Double.isNaN(orig);

        final double newDouble = orig * multiplier;

        final Value newValue = new Value(newDouble);

        return newValue;
    }

}
