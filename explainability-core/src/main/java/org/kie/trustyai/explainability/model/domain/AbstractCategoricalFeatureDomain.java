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
package org.kie.trustyai.explainability.model.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

@Entity
public class AbstractCategoricalFeatureDomain<T> extends FeatureDomain<T> {
    protected final Set<T> categories;

    protected AbstractCategoricalFeatureDomain(Set<T> categories) {
        this.categories = categories;
    }

    public AbstractCategoricalFeatureDomain() {
        categories = new HashSet<>();
    }

    @Override
    @Transient
    public boolean isEmpty() {
        return false;
    }

    @Override
    @Transient
    public Double getLowerBound() {
        return null;
    }

    @Override
    @Transient
    public Double getUpperBound() {
        return null;
    }

    @Override
    @Transient
    public Set<T> getCategories() {
        return this.categories;
    }

}
