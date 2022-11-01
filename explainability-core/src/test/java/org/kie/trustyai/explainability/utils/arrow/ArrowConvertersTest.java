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
package org.kie.trustyai.explainability.utils.arrow;

import java.util.ArrayList;
import java.util.List;

import org.apache.arrow.memory.RootAllocator;
import org.apache.arrow.vector.VectorSchemaRoot;
import org.apache.arrow.vector.types.pojo.Schema;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;

import static org.junit.jupiter.api.Assertions.*;

class ArrowConvertersTest {
    double[][] mat = {
            { 5., 6., 7., -4., 8. },
            { 11., 12., 13., -5., 14. },
            { 0., 0, 1., 4., 2. },
    };

    private List<PredictionInput> createPIFromMatrix(double[][] m) {
        List<PredictionInput> pis = new ArrayList<>();
        int[] shape = new int[] { m.length, m[0].length };
        for (int i = 0; i < shape[0]; i++) {
            List<Feature> fs = new ArrayList<>();
            for (int j = 0; j < shape[1]; j++) {
                fs.add(FeatureFactory.newNumericalFeature("f", m[i][j]));
            }
            pis.add(new PredictionInput(fs));
        }
        return pis;
    }

    @Test
    public void scratch1() {
        List<PredictionInput> matPI = createPIFromMatrix(mat);
        Schema prototype = ArrowConverters.generatePrototypePISchema(matPI.get(0));
        RootAllocator sourceRootAlloc = new RootAllocator(Integer.MAX_VALUE);
        VectorSchemaRoot vsr = ArrowConverters.convertPItoVSR(matPI, prototype, sourceRootAlloc);
        System.out.println(vsr.contentToTSVString());
    }
}
