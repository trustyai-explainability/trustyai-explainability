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
package org.kie.trustyai.explainability.local.counterfactual.entities;

import java.util.ArrayList;
import java.util.Set;

import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.entity.PlanningPin;
import ai.timefold.solver.core.api.domain.valuerange.CountableValueRange;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;
import ai.timefold.solver.core.impl.domain.valuerange.buildin.collection.ListValueRange;

/**
 * Mapping between a numerical categorical feature an Timefold Solver {@link PlanningEntity}
 */
@PlanningEntity
public class CategoricalNumericalEntity extends AbstractCategoricalEntity<Integer> {

    public CategoricalNumericalEntity() {
        super();
    }

    private CategoricalNumericalEntity(Integer originalValue, String featureName, Set<Integer> allowedCategories, boolean constrained) {
        super(originalValue, featureName, allowedCategories, constrained);
    }

    /**
     * Creates a {@link CategoricalNumericalEntity}, taking the original input value from the
     * provided {@link Feature} and specifying whether the entity is constrained or not.
     * A set of allowed category values must be passed.
     *
     * @param originalFeature Original input {@link Feature}
     * @param categories Set of allowed category values
     * @param constrained Whether this entity's value should be fixed or not
     */
    public static CategoricalNumericalEntity from(Feature originalFeature, Set<Integer> categories, boolean constrained) {
        return new CategoricalNumericalEntity((Integer) originalFeature.getValue().getUnderlyingObject(), originalFeature.getName(),
                categories, constrained);
    }

    /**
     * Creates an unconstrained {@link CategoricalNumericalEntity}, taking the original input value from the
     * provided {@link Feature}.
     * A set of allowed category values must be passed.
     *
     * @param originalFeature feature Original input {@link Feature}
     * @param categories Set of allowed category values
     */
    public static CategoricalNumericalEntity from(Feature originalFeature, Set<Integer> categories) {
        return CategoricalNumericalEntity.from(originalFeature, categories, false);
    }

    @ValueRangeProvider(id = "categoricalNumericalRange")
    public CountableValueRange<Integer> getValueRange() {
        return new ListValueRange<>(new ArrayList<>(allowedCategories));
    }

    /**
     * Returns the {@link CategoricalNumericalEntity} as a {@link Feature}
     *
     * @return {@link Feature}
     */
    @Override
    public Feature asFeature() {
        return FeatureFactory.newCategoricalNumericalFeature(featureName, this.proposedValue);
    }

    @Override
    @PlanningVariable(valueRangeProviderRefs = { "categoricalNumericalRange" })
    public Integer getProposedValue() {
        return proposedValue;
    }

    @Override
    public void setProposedValue(Integer proposedValue) {
        this.proposedValue = proposedValue;
    }

    @Override
    @PlanningPin
    public boolean isConstrained() {
        return constrained;
    }

}
