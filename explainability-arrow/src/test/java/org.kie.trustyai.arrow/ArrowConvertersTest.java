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
package org.kie.trustyai.arrow;

import java.util.List;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;

import static org.junit.jupiter.api.Assertions.*;

class ArrowConvertersTest {
    List<PredictionInput> pis = List.of(
            new PredictionInput(List.of(
                    FeatureFactory.newTextFeature("text", "string1"),
                    FeatureFactory.newNumericalFeature("float", 5.),
                    FeatureFactory.newBooleanFeature("bool", false),
                    FeatureFactory.newCategoricalFeature("categorical", "category"))),
            new PredictionInput(List.of(
                    FeatureFactory.newTextFeature("text", "string2"),
                    FeatureFactory.newNumericalFeature("float", 6.),
                    FeatureFactory.newBooleanFeature("bool", true),
                    FeatureFactory.newCategoricalFeature("categorical", "category"))),
            new PredictionInput(List.of(
                    FeatureFactory.newTextFeature("text", "long complicated string"),
                    FeatureFactory.newNumericalFeature("float", 7.),
                    FeatureFactory.newBooleanFeature("bool", false),
                    FeatureFactory.newCategoricalFeature("categorical", "category"))));

    @Test
    void testReadWrite() {
        Schema prototype = ArrowConverters.generatePrototypePISchema(pis.get(0));
        RootAllocator sourceRootAlloc = new RootAllocator(Integer.MAX_VALUE);
        VectorSchemaRoot vsr = ArrowConverters.convertPItoVSR(pis, prototype, sourceRootAlloc);
        byte[] buffer = ArrowConverters.write(vsr);
        List<PredictionOutput> outputs = ArrowConverters.read(buffer, sourceRootAlloc);
        for (int i = 0; i < pis.size(); i++) {
            for (int j = 0; j < pis.get(0).getFeatures().size(); j++) {
                assertEquals(pis.get(i).getFeatures().get(j).getName(), outputs.get(i).getOutputs().get(j).getName());
                assertEquals(pis.get(i).getFeatures().get(j).getType(), outputs.get(i).getOutputs().get(j).getType());
                assertEquals(pis.get(i).getFeatures().get(j).getValue().getUnderlyingObject(), outputs.get(i).getOutputs().get(j).getValue().getUnderlyingObject());
            }
        }
    }
}
