/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kie.trustyai.explainability;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.kie.trustyai.explainability.local.lime.LimeExplainer;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.AsyncPredictionProvider;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.utils.ValidationUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {

    public static Feature getMockedNumericFeature() {
        return getMockedNumericFeature(1d);
    }

    public static Feature getMockedFeature(Type type, Value v) {
        Feature f = mock(Feature.class);
        when(f.getType()).thenReturn(type);
        when(f.getName()).thenReturn("f-" + type.name());
        when(f.getValue()).thenReturn(v);
        return f;
    }

    public static Feature getMockedTextFeature(String s) {
        Feature f = mock(Feature.class);
        when(f.getType()).thenReturn(Type.TEXT);
        when(f.getName()).thenReturn("f-text");
        Value value = mock(Value.class);
        when(value.getUnderlyingObject()).thenReturn(s);
        when(value.asNumber()).thenReturn(Double.NaN);
        when(value.asString()).thenReturn(s);
        when(f.getValue()).thenReturn(value);
        return f;
    }

    public static Feature getMockedNumericFeature(double d) {
        Feature f = mock(Feature.class);
        when(f.getType()).thenReturn(Type.NUMBER);
        when(f.getName()).thenReturn("f-num");
        Value value = mock(Value.class);
        when(value.getUnderlyingObject()).thenReturn(d);
        when(value.asNumber()).thenReturn(d);
        when(value.asString()).thenReturn(String.valueOf(d));
        when(f.getValue()).thenReturn(value);
        return f;
    }

    public static void assertLimeStability(AsyncPredictionProvider model, Prediction prediction, LimeExplainer limeExplainer,
                                           int topK, double minimumPositiveStabilityRate, double minimumNegativeStabilityRate) {
        assertDoesNotThrow(() -> ValidationUtils.validateLocalSaliencyStability(model, prediction, limeExplainer, topK,
                minimumPositiveStabilityRate, minimumNegativeStabilityRate));
    }

    public static void fillBalancedDataForFiltering(int size, List<Pair<double[], Double>> trainingSet, double[] weights) {
        for (int i = 0; i < size; i++) {
            double[] x = new double[2];
            for (int j = 0; j < 2; j++) {
                x[j] = (i + j) % 2 == 0 ? 0 : 1;
            }
            Double y = i % 3 == 0 ? 0d : 1d;
            trainingSet.add(Pair.of(x, y));
            weights[i] = i % 2 == 0 ? 0.2 : 0.8;
        }
    }
}
