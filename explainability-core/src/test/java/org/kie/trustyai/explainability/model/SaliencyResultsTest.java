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

    SaliencyResults buildSaliencyResults(int nOutputs, int nFeatures, int scalar1, int scalar2, SaliencyResults.SourceExplainer source) {
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
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, SaliencyResults.SourceExplainer.SHAP);
        assertEquals(sr1, sr1);
        assertEquals(sr1.hashCode(), sr1.hashCode());
    }

    @Test
    void testEquals() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, SaliencyResults.SourceExplainer.LIME);
        SaliencyResults sr2 = buildSaliencyResults(2, 2, 1, 1, SaliencyResults.SourceExplainer.LIME);
        assertEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffOutputs() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, SaliencyResults.SourceExplainer.SHAP);
        SaliencyResults sr2 = buildSaliencyResults(20, 2, 1, 1, SaliencyResults.SourceExplainer.SHAP);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffFeatures() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, SaliencyResults.SourceExplainer.SHAP);
        SaliencyResults sr2 = buildSaliencyResults(2, 20, 1, 1, SaliencyResults.SourceExplainer.SHAP);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffImportances() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, SaliencyResults.SourceExplainer.SHAP);
        SaliencyResults sr2 = buildSaliencyResults(2, 2, 10, 1, SaliencyResults.SourceExplainer.SHAP);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffFnull() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 1, SaliencyResults.SourceExplainer.SHAP);
        SaliencyResults sr2 = buildSaliencyResults(2, 2, 1, 10, SaliencyResults.SourceExplainer.SHAP);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void testDiffSource() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 10, SaliencyResults.SourceExplainer.SHAP);
        SaliencyResults sr2 = buildSaliencyResults(2, 2, 1, 10, SaliencyResults.SourceExplainer.LIME);
        assertNotEquals(sr1, sr2);
        assertNotEquals(sr1.hashCode(), sr2.hashCode());
    }

    @Test
    void difference() {
        SaliencyResults sr1 = buildSaliencyResults(1, 5, 1, 10, SaliencyResults.SourceExplainer.SHAP);
        SaliencyResults sr2 = buildSaliencyResults(1, 5, 2, 11, SaliencyResults.SourceExplainer.SHAP);

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
    void processAvailableCounterfactuals() {
        PredictionOutput po1 = new PredictionOutput(List.of(new Output("o1", Type.NUMBER, new Value(1), 1.0)));
        PredictionOutput po2 = new PredictionOutput(List.of(new Output("o1", Type.NUMBER, new Value(2), 1.0)));
        PredictionOutput po3 = new PredictionOutput(List.of(new Output("o1", Type.NUMBER, new Value(2), 1.0)));

        PredictionInput originalpPI = new PredictionInput(
                List.of(new Feature("f1", Type.NUMBER, new Value(1)),
                        new Feature("f2", Type.NUMBER, new Value(1)),
                        new Feature("f3", Type.NUMBER, new Value(1))));
        HashMap<PredictionOutput, PredictionInput> availableCFs = new HashMap<>();
        PredictionInput pi1 = new PredictionInput(
                List.of(new Feature("f1", Type.NUMBER, new Value(1)),
                        new Feature("f2", Type.NUMBER, new Value(1)),
                        new Feature("f3", Type.NUMBER, new Value(2))));
        PredictionInput pi2 = new PredictionInput(
                List.of(new Feature("f1", Type.NUMBER, new Value(1)),
                        new Feature("f2", Type.NUMBER, new Value(2)),
                        new Feature("f3", Type.NUMBER, new Value(2))));
        PredictionInput pi3 = new PredictionInput(
                List.of(new Feature("f1", Type.NUMBER, new Value(2)),
                        new Feature("f2", Type.NUMBER, new Value(2)),
                        new Feature("f3", Type.NUMBER, new Value(2))));

        SimplePrediction sp1 = new SimplePrediction(pi1, po1);
        SimplePrediction sp2 = new SimplePrediction(pi2, po2);
        SimplePrediction sp3 = new SimplePrediction(pi3, po3);

        List<SimplePrediction> availablePredictionPairs = List.of(sp1, sp2, sp3);

        Map<SimplePrediction, List<Boolean>> procCFs =
                SaliencyResults.processAvailableCounterfactuals(originalpPI, availablePredictionPairs);

        // pi1 checks
        assertEquals(false, procCFs.get(sp1).get(0));
        assertEquals(false, procCFs.get(sp1).get(1));
        assertEquals(true, procCFs.get(sp1).get(2));

        // pi2 checks
        assertEquals(false, procCFs.get(sp2).get(0));
        assertEquals(true, procCFs.get(sp2).get(1));
        assertEquals(true, procCFs.get(sp2).get(2));

        // pi3 checks
        assertEquals(true, procCFs.get(sp3).get(0));
        assertEquals(true, procCFs.get(sp3).get(1));
        assertEquals(true, procCFs.get(sp3).get(2));
    }

    @Test
    void testAsTableSHAP() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 10, SaliencyResults.SourceExplainer.SHAP);
        String tableString = sr1.asTable();
        assertTrue(tableString.contains("FNull"));
        assertTrue(tableString.contains("SHAP Values"));
    }

    @Test
    void testAsTableLIME() {
        SaliencyResults sr1 = buildSaliencyResults(2, 2, 1, 10, SaliencyResults.SourceExplainer.LIME);
        String tableString = sr1.asTable();
        assertFalse(tableString.contains("FNull"));
        assertTrue(tableString.contains("Saliency"));
    }
}
