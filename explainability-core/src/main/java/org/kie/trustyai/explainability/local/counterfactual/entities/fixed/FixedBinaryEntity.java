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
package org.kie.trustyai.explainability.local.counterfactual.entities.fixed;

import java.nio.ByteBuffer;

import org.kie.trustyai.explainability.local.counterfactual.entities.AbstractEntity;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;

/**
 * OptaPlanner representation of a fixed binary feature
 */

public class FixedBinaryEntity extends AbstractEntity<ByteBuffer> {

    public FixedBinaryEntity() {
        super();
    }

    private FixedBinaryEntity(ByteBuffer originalValue, String featureName) {
        super(originalValue, featureName, true);
    }

    /**
     * Creates a {@link FixedBinaryEntity}, taking the original input value from the
     * provided {@link Feature}.
     *
     * @param originalFeature Original input {@link Feature}
     */
    public static FixedBinaryEntity from(Feature originalFeature) {
        return new FixedBinaryEntity((ByteBuffer) originalFeature.getValue().getUnderlyingObject(), originalFeature.getName());
    }

    /**
     * The distance between the current planning value and the reference value
     * for this feature. Since this is a fixed entity, the distance will always be zero.
     *
     * @return Numerical distance (constant and zero)
     */
    @Override
    public double distance() {
        return 0.0;
    }

    @Override
    public double similarity() {
        return 1.0;
    }

    /**
     * Returns the {@link FixedBinaryEntity} as a {@link Feature}
     *
     * @return {@link Feature}
     */
    @Override
    public Feature asFeature() {
        return FeatureFactory.newBinaryFeature(this.featureName, this.proposedValue);
    }
}
