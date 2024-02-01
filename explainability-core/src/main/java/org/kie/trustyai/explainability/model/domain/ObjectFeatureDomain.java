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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import org.kie.trustyai.explainability.model.UnderlyingObject;

@Entity
public class ObjectFeatureDomain extends AbstractCategoricalFeatureDomain<UnderlyingObject> {

    private ObjectFeatureDomain(Set<UnderlyingObject> categories) {
        super(categories);
    }

    public ObjectFeatureDomain() {
        super(new HashSet<>());
    }

    /**
     * Create a {@link FeatureDomain} for a categorical feature
     *
     * @param categories A set with all the allowed category values
     * @return A {@link FeatureDomain}
     */
    public static ObjectFeatureDomain create(Set<Object> categories) {
        return new ObjectFeatureDomain(categories.stream().map(UnderlyingObject::new).collect(Collectors.toSet()));
    }

    public static ObjectFeatureDomain create(List<Object> categories) {
        return new ObjectFeatureDomain(categories.stream().map(UnderlyingObject::new).collect(Collectors.toSet()));
    }

    public static ObjectFeatureDomain create(Object... categories) {
        return new ObjectFeatureDomain(Arrays.stream(categories).map(UnderlyingObject::new).collect(Collectors.toSet()));
    }

    @ElementCollection
    @Access(AccessType.FIELD)
    @Override
    public Set<UnderlyingObject> getCategories() {
        return this.categories;
    }

    @Transient
    public Set<Object> getRawCategories() {
        return this.categories.stream().map(UnderlyingObject::getObject).collect(Collectors.toSet());
    }
}
