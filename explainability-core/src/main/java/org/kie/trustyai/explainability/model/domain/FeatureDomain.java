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

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Information about the search space domain for the model's features.
 */

@Entity
public interface FeatureDomain<T> {
    String id = String.valueOf(UUID.randomUUID());

    @Id
    @Access(AccessType.FIELD)
    default String getId() {
        return id;
    }

    /**
     * Return whether this is an empty domain
     *
     * @return True if empty
     */
    @Access(AccessType.FIELD)
    boolean isEmpty();

    /**
     * Get start value for this boundary
     *
     * @return the start value
     */
    @Access(AccessType.FIELD)
    Double getLowerBound();

    /**
     * Get the end value for this boundary
     *
     * @return the end value
     */
    @Access(AccessType.FIELD)
    Double getUpperBound();

    /**
     * Get the possible values for this boundary
     *
     * @return the end value
     */
    @Access(AccessType.FIELD)
    @ElementCollection
    Set<T> getCategories();

    /**
     * Get a string representation of the domain
     *
     * @return the string representation
     */
    default String prettyPrint() {
        if (getCategories() == null && getLowerBound() != null && getUpperBound() != null) {
            return String.format("%f->%f", getLowerBound(), getUpperBound());
        } else if (getCategories() != null) {
            return getCategories().toString();
        } else {
            return "null";
        }
    }
}
