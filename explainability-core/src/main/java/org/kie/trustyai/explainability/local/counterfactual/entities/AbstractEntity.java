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
package org.kie.trustyai.explainability.local.counterfactual.entities;

import java.util.Objects;

import org.kie.trustyai.explainability.model.Feature;
import org.optaplanner.core.api.domain.valuerange.ValueRange;
import org.optaplanner.core.impl.domain.valuerange.buildin.composite.EmptyValueRange;

/**
 * Common class for counterfactual entities
 */
public abstract class AbstractEntity<T> implements CounterfactualEntity {

    protected T proposedValue;
    protected String featureName;

    protected boolean constrained;
    protected T originalValue;

    protected AbstractEntity() {
    }

    protected AbstractEntity(T originalValue, String featureName, boolean constrained) {
        this.proposedValue = originalValue;
        this.originalValue = originalValue;
        this.featureName = featureName;
        this.constrained = constrained;
    }

    @Override
    public boolean isConstrained() {
        return constrained;
    }

    /**
     * Returns whether the {@link DoubleEntity} new value is different from the reference
     * {@link Feature} value.
     *
     * @return boolean
     */
    @Override
    public boolean isChanged() {
        return !Objects.equals(originalValue, proposedValue);
    }

    @Override
    public String toString() {
        return originalValue.getClass().getName() + "Entity{"
                + "value="
                + proposedValue
                + ", id='"
                + featureName
                + '\''
                + '}';
    }

    @Override
    public ValueRange getValueRange() {
        return new EmptyValueRange<>();
    }

    @Override
    public T getProposedValue() {
        return proposedValue;
    }

    public void setProposedValue(T proposedValue) {
        this.proposedValue = proposedValue;
    }
}
