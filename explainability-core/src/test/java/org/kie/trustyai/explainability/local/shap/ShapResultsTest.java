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

package org.kie.trustyai.explainability.local.shap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.FeatureImportance;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Saliency;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ShapResultsTest {
    ShapResults buildShapResults(int nOutputs, int nFeatures, int scalar1, int scalar2) {
        Map<String, Saliency> saliencies = new HashMap<>();
        Map<String, Double> fnull = new HashMap<>();
        for (int i = 0; i < nOutputs; i++) {
            List<FeatureImportance> fis = new ArrayList<>();
            fis.add(new FeatureImportance(FeatureFactory.newNumericalFeature("Background", (double) scalar2), scalar2));
            for (int j = 0; j < nFeatures; j++) {
                fis.add(new FeatureImportance(new Feature("Feature " + String.valueOf(j), Type.NUMBER, new Value(j)), (i + 1) * j * scalar1));
            }

            String oname = "Output " + i;
            saliencies.put(oname,
                    new Saliency(new Output(oname, Type.NUMBER, new Value(i + 1), 1.0), fis));
        }
        return new ShapResults(saliencies);
    }

    // test equals and hashing
    @Test
    void testEqualsSameObj() {
        ShapResults sr1 = buildShapResults(2, 2, 1, 1);
        assertEquals(sr1, sr1);
        assertEquals(sr1.hashCode(), sr1.hashCode());
    }

    @Test
    void testEquals() {
        ShapResults sr1 = buildShapResults(2, 2, 1, 1);
        ShapResults sr2 = buildShapResults(2, 2, 1, 1);
        assertEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffOutputs() {
        ShapResults sr1 = buildShapResults(2, 2, 1, 1);
        ShapResults sr2 = buildShapResults(20, 2, 1, 1);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffFeatures() {
        ShapResults sr1 = buildShapResults(2, 2, 1, 1);
        ShapResults sr2 = buildShapResults(2, 20, 1, 1);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffImportances() {
        ShapResults sr1 = buildShapResults(2, 2, 1, 1);
        ShapResults sr2 = buildShapResults(2, 2, 10, 1);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffFnull() {
        ShapResults sr1 = buildShapResults(2, 2, 1, 1);
        ShapResults sr2 = buildShapResults(2, 2, 1, 10);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }
}
