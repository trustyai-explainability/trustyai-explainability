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

import java.util.Collections;
import java.util.List;

import org.kie.trustyai.explainability.model.domain.FeatureDomain;

/**
 * Information about feature domains of data used for training a model.
 */
public class DataDomain {

    private final List<FeatureDomain> featureDomains;

    public DataDomain(List<FeatureDomain> featureDomains) {
        this.featureDomains = Collections.unmodifiableList(featureDomains);
    }

    /**
     * Get all feature data domains
     *
     * @return A list of {@link FeatureDomain}
     */
    public List<FeatureDomain> getFeatureDomains() {
        return featureDomains;
    }
}