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

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureDistribution;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.domain.BinaryFeatureDomain;
import org.kie.trustyai.explainability.model.domain.CategoricalFeatureDomain;
import org.kie.trustyai.explainability.model.domain.CategoricalNumericalFeatureDomain;
import org.kie.trustyai.explainability.model.domain.CurrencyFeatureDomain;
import org.kie.trustyai.explainability.model.domain.DurationFeatureDomain;
import org.kie.trustyai.explainability.model.domain.FeatureDomain;
import org.kie.trustyai.explainability.model.domain.ObjectFeatureDomain;
import org.kie.trustyai.explainability.model.domain.URIFeatureDomain;
import org.kie.trustyai.explainability.utils.CompositeFeatureUtils;

public class CounterfactualEntityFactory {

    private static final String UNSUPPORTED_TYPE_MESSAGE = "Unsupported feature type: ";

    private CounterfactualEntityFactory() {
    }

    public static CounterfactualEntity from(Feature feature) {
        return CounterfactualEntityFactory.from(feature, null);
    }

    public static CounterfactualEntity from(Feature feature, FeatureDistribution featureDistribution) {
        CounterfactualEntity entity = null;
        validateFeature(feature);
        final Type type = feature.getType();
        final FeatureDomain featureDomain = feature.getDomain();
        final boolean isConstrained = feature.isConstrained();
        final Object valueObject = feature.getValue().getUnderlyingObject();
        if (type == Type.NUMBER) {
            if (valueObject instanceof Double) {
                if (isConstrained) {
                    entity = DoubleEntity.from(feature, (double) valueObject, (double) valueObject,
                            null, true);
                } else {
                    entity = DoubleEntity.from(feature, featureDomain.getLowerBound(), featureDomain.getUpperBound(),
                            featureDistribution, isConstrained);
                }
            } else if (valueObject instanceof Long) {
                if (isConstrained) {
                    entity = LongEntity.from(feature, (long) valueObject,
                            (long) valueObject, null, true);
                } else {
                    entity = LongEntity.from(feature, featureDomain.getLowerBound().intValue(),
                            featureDomain.getUpperBound().intValue(), featureDistribution, isConstrained);
                }
            } else if (valueObject instanceof Integer) {
                if (isConstrained) {
                    entity = IntegerEntity.from(feature, (int) valueObject,
                            (int) valueObject, null, true);
                } else {
                    entity = IntegerEntity.from(feature, featureDomain.getLowerBound().intValue(),
                            featureDomain.getUpperBound().intValue(), featureDistribution, isConstrained);
                }
            }
        } else if (feature.getType() == Type.BOOLEAN) {
            entity = BooleanEntity.from(feature, isConstrained);
        } else if (feature.getType() == Type.TEXT) {
            if (isConstrained) {
                entity = TextEntity.from(feature, null, true);
            } else {
                throw new IllegalArgumentException(UNSUPPORTED_TYPE_MESSAGE + feature.getType());
            }
        } else if (feature.getType() == Type.BINARY) {
            if (isConstrained) {
                entity = BinaryEntity.from(feature, null, true);
            } else {
                entity = BinaryEntity.from(feature, ((BinaryFeatureDomain) featureDomain).getCategories(), isConstrained);
            }
        } else if (feature.getType() == Type.URI) {
            if (isConstrained) {
                entity = URIEntity.from(feature, null, true);
            } else {
                entity = URIEntity.from(feature, ((URIFeatureDomain) featureDomain).getCategories(), isConstrained);
            }
        } else if (feature.getType() == Type.TIME) {
            if (isConstrained) {
                entity = TimeEntity.from(feature, (LocalTime) valueObject, (LocalTime) valueObject, true);
            } else {
                final LocalTime lowerBound = LocalTime.MIN.plusSeconds(featureDomain.getLowerBound().longValue());
                final LocalTime upperBound = LocalTime.MIN.plusSeconds(featureDomain.getUpperBound().longValue());
                entity = TimeEntity.from(feature, lowerBound, upperBound, isConstrained);
            }
        } else if (feature.getType() == Type.DURATION) {
            if (isConstrained) {
                entity = DurationEntity.from(feature,
                        (Duration) valueObject,
                        (Duration) valueObject,
                        null,
                        true);
            } else {
                DurationFeatureDomain domain = (DurationFeatureDomain) featureDomain;
                entity = DurationEntity.from(feature, Duration.of(domain.getLowerBound().longValue(), domain.getUnit()),
                        Duration.of(domain.getUpperBound().longValue(), domain.getUnit()),
                        featureDistribution, isConstrained);
            }
        } else if (feature.getType() == Type.VECTOR) {
            throw new IllegalArgumentException(UNSUPPORTED_TYPE_MESSAGE + feature.getType());
        } else if (feature.getType() == Type.COMPOSITE) {
            throw new IllegalArgumentException(UNSUPPORTED_TYPE_MESSAGE + feature.getType());
        } else if (feature.getType() == Type.CURRENCY) {
            if (isConstrained) {
                entity = CurrencyEntity.from(feature, null, true);
            } else {
                entity = CurrencyEntity.from(feature, ((CurrencyFeatureDomain) featureDomain).getCategories(), isConstrained);
            }
        } else if (feature.getType() == Type.CATEGORICAL) {
            if (isConstrained) {
                entity = CategoricalEntity.from(feature, null, true);
            } else if (featureDomain instanceof BinaryFeatureDomain) {
                entity = BinaryEntity.from(feature, ((BinaryFeatureDomain) featureDomain).getCategories());
            } else if (featureDomain instanceof CurrencyFeatureDomain) {
                entity = CurrencyEntity.from(feature, ((CurrencyFeatureDomain) featureDomain).getCategories());
            } else if (featureDomain instanceof ObjectFeatureDomain) {
                entity = ObjectEntity.from(feature, ((ObjectFeatureDomain) featureDomain).getRawCategories());
            } else if (featureDomain instanceof URIFeatureDomain) {
                entity = URIEntity.from(feature, ((URIFeatureDomain) featureDomain).getCategories());
            } else if (featureDomain instanceof CategoricalNumericalFeatureDomain) {
                entity = CategoricalNumericalEntity.from(feature, ((CategoricalNumericalFeatureDomain) featureDomain).getCategories());
            } else {
                entity = CategoricalEntity.from(feature, ((CategoricalFeatureDomain) featureDomain).getCategories());
            }
        } else if (feature.getType() == Type.UNDEFINED) {
            if (isConstrained) {
                entity = ObjectEntity.from(feature, null, true);
            } else {
                entity = ObjectEntity.from(feature, ((ObjectFeatureDomain) featureDomain).getRawCategories(), isConstrained);
            }
        } else {
            throw new IllegalArgumentException(UNSUPPORTED_TYPE_MESSAGE + feature.getType());
        }
        return entity;
    }

    /**
     * Validation of features for counterfactual entity construction
     *
     * @param feature {@link Feature} to be validated
     */
    public static void validateFeature(Feature feature) {
        final Type type = feature.getType();
        final Object object = feature.getValue().getUnderlyingObject();
        if (type == Type.NUMBER) {
            if (object == null) {
                throw new IllegalArgumentException("Null numeric features are not supported in counterfactuals");
            }
        }
    }

    public static List<CounterfactualEntity> createEntities(PredictionInput predictionInput) {
        final List<Feature> linearizedFeatures = CompositeFeatureUtils.flattenFeatures(predictionInput.getFeatures());
        return linearizedFeatures.stream().map(
                (Feature feature) -> CounterfactualEntityFactory.from(feature, feature.getDistribution()))
                .collect(Collectors.toList());
    }
}
