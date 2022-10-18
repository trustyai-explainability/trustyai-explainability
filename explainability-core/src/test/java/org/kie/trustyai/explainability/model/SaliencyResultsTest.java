/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SaliencyResultsTest {

    SaliencyResults buildSaliencyResults(int nOutputs, int nFeatures, int scalar1, int scalar2, String source) {
        Map<String, Saliency> saliencies = new HashMap<>();
        Map<String, Double> fnull = new HashMap<>();
        for (int i = 0; i < nOutputs; i++) {
            List<FeatureImportance> fis = new ArrayList<>();

            for (int j = 0; j < nFeatures; j++) {
                fis.add(new FeatureImportance(new Feature("Feature " + String.valueOf(j), Type.NUMBER, new Value(j)), (i + 1) * j * scalar1));
            }
            fis.add(new FeatureImportance(FeatureFactory.newNumericalFeature("Background", (double) scalar2), scalar2));
            String oname = "Output " + i;
            saliencies.put(oname,
                    new Saliency(new Output(oname, Type.NUMBER, new Value(i + 1), 1.0), fis));
        }
        return new SaliencyResults(saliencies, source);
    }

    // test equals and hashing
    @Test
    void testEqualsSameObj() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, "SHAP");
        assertEquals(sr1, sr1);
        assertEquals(sr1.hashCode(), sr1.hashCode());
    }

    @Test
    void testEquals() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, "LIME");
        SaliencyResults sr2 = buildSaliencyResults(2, 2, 1, 1, "LIME");
        assertEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffOutputs() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, "SHAP");
        SaliencyResults sr2 = buildSaliencyResults(20, 2, 1, 1, "SHAP");
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffFeatures() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, "SHAP");
        SaliencyResults sr2 = buildSaliencyResults(2, 20, 1, 1, "SHAP");
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffImportances() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, "SHAP");
        SaliencyResults sr2 = buildSaliencyResults(2, 2, 10, 1, "SHAP");
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffFnull() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, "SHAP");
        SaliencyResults sr2 = buildSaliencyResults(2, 2, 1, 10, "SHAP");
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffSource() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 10, "SHAP");
        SaliencyResults sr2 = buildSaliencyResults(2, 2, 1, 10, "LIME");
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void difference() {
        SaliencyResults sr1 = buildSaliencyResults(1, 5, 1, 10, "SHAP");
        SaliencyResults sr2 = buildSaliencyResults(1, 5, 2, 11, "SHAP");

        SaliencyResults srDiff = sr2.difference(sr1);

        List<FeatureImportance> saliencyDiff = srDiff.getSaliencies().get("Output 0").getPerFeatureImportance();
        assertEquals(0 - 0, saliencyDiff.get(0).getScore());
        assertEquals(2 - 1, saliencyDiff.get(1).getScore());
        assertEquals(4 - 2, saliencyDiff.get(2).getScore());
        assertEquals(6 - 3, saliencyDiff.get(3).getScore());
        assertEquals(8 - 4, saliencyDiff.get(4).getScore());
        assertEquals(11 - 10, saliencyDiff.get(5).getScore());

    }

    @Test
    void testAsTableSHAP() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 10, "SHAP");
        String tableString = sr1.asTable();
        assertTrue(tableString.contains("FNull"));
        assertTrue(tableString.contains("SHAP Values"));
    }

    @Test
    void testAsTableLIME() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 10, "LIME");
        String tableString = sr1.asTable();
        assertFalse(tableString.contains("FNull"));
        assertTrue(tableString.contains("Saliency"));
    }
}
