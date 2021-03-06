/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates.
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
package org.kie.trustyai.explainability.model;

import java.util.Random;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureTest {

    private final PerturbationContext context = new PerturbationContext(new Random(), 1);

    @ParameterizedTest
    @EnumSource
    void testEquality(Type type) {
        String name = "some_name";
        Value value1 = type.randomValue(context);
        Value value2 = new Value(value1.getUnderlyingObject());
        Feature feature1 = new Feature(name, type, value1);
        Feature feature2 = new Feature(name, type, value2);
        assertThat(feature1).isEqualTo(feature2);
    }
}
