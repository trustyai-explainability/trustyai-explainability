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

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.domain.CategoricalFeatureDomain;
import org.kie.trustyai.explainability.model.domain.EmptyFeatureDomain;
import org.kie.trustyai.explainability.model.domain.FeatureDomain;
import org.kie.trustyai.explainability.model.domain.NumericalFeatureDomain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeatureDomainTest {

    @Test
    void getCategories() {
        final FeatureDomain domain = CategoricalFeatureDomain.create("foo", "bar", "foo", "bar", "bar");
        assertEquals(Set.of("foo", "bar"), domain.getCategories());
    }

    @Test
    void isEmpty() {
        final FeatureDomain domain = EmptyFeatureDomain.create();
        assertNull(domain.getCategories());
        assertNull(domain.getLowerBound());
        assertNull(domain.getUpperBound());
        assertTrue(domain.isEmpty());
    }

    @Test
    void getStart() {
        final FeatureDomain domain = NumericalFeatureDomain.create(0.0, 10.0);
        assertEquals(0.0, domain.getLowerBound());
    }

    @Test
    void getEnd() {
        final FeatureDomain domain = NumericalFeatureDomain.create(-10, -5);
        assertEquals(-5.0, domain.getUpperBound());
    }
}
