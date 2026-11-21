package org.kie.trustyai.explainability.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.util.Pair;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IOUtilsTest {

    private List<Feature> generateFeatures(int n, String suffix, Type type, int valueOffset) {
        List<Feature> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(new Feature("f" + i + suffix, type, new Value(i + valueOffset)));
        }
        return result;
    }

    @Test
    void featureListComparisonDifferentVals() {
        List<Feature> f1 = generateFeatures(10, "", Type.NUMBER, 0);
        List<Feature> f2 = generateFeatures(10, "", Type.NUMBER, 1);
        Pair<String, Integer> tableAndWidth = IOUtils.featureListComparison(f1, f2, "Feature List 1", "Feature List 2", true);
        assertEquals("=== Feature Mismatches ==================\n" +
                " Index | Feature List 1 != Feature List 2\n" +
                "-----------------------------------------\n" +
                "     0 |        Value=0 !=        Value=1\n" +
                "     1 |        Value=1 !=        Value=2\n" +
                "     2 |        Value=2 !=        Value=3\n" +
                "     3 |        Value=3 !=        Value=4\n" +
                "     4 |        Value=4 !=        Value=5\n" +
                "     5 |        Value=5 !=        Value=6\n" +
                "     6 |        Value=6 !=        Value=7\n" +
                "     7 |        Value=7 !=        Value=8\n" +
                "     8 |        Value=8 !=        Value=9\n" +
                "     9 |        Value=9 !=       Value=10\n" +
                "=========================================", tableAndWidth.getFirst());
    }

    @Test
    void featureListComparisonDifferentValsIgnoreVal() {
        List<Feature> f1 = generateFeatures(10, "", Type.NUMBER, 0);
        List<Feature> f2 = generateFeatures(10, "", Type.NUMBER, 1);
        Pair<String, Integer> tableAndWidth = IOUtils.featureListComparison(f1, f2, "Feature List 1", "Feature List 2", false);
        assertEquals("=== Feature Mismatches ==================\n" +
                " Index | Feature List 1 != Feature List 2\n" +
                "-----------------------------------------\n" +
                "=========================================", tableAndWidth.getFirst());
    }

    @Test
    void featureListComparisonDifferentTypes() {
        List<Feature> f1 = generateFeatures(10, "", Type.NUMBER, 0);
        List<Feature> f2 = generateFeatures(10, "", Type.CATEGORICAL, 0);
        Pair<String, Integer> tableAndWidth = IOUtils.featureListComparison(f1, f2, "Feature List 1", "Feature List 2", true);
        assertEquals("=== Feature Mismatches ===================================\n" +
                " Index |       Feature List 1 !=            Feature List 2\n" +
                "----------------------------------------------------------\n" +
                "     0 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     1 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     2 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     3 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     4 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     5 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     6 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     7 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     8 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "     9 | TrustyAI Type=number != TrustyAI Type=categorical\n" +
                "==========================================================", tableAndWidth.getFirst());
    }

    @Test
    void featureListComparisonDifferentNames() {
        List<Feature> f1 = generateFeatures(10, "_suf", Type.NUMBER, 0);
        List<Feature> f2 = generateFeatures(10, "", Type.CATEGORICAL, 0);
        Pair<String, Integer> tableAndWidth = IOUtils.featureListComparison(f1, f2, "Feature List 1", "Feature List 2", true);
        assertEquals("=== Feature Mismatches =========================================================\n" +
                " Index |                    Feature List 1 !=                     Feature List 2\n" +
                "--------------------------------------------------------------------------------\n" +
                "     0 | Name=f0_suf, TrustyAI Type=number != Name=f0, TrustyAI Type=categorical\n" +
                "     1 | Name=f1_suf, TrustyAI Type=number != Name=f1, TrustyAI Type=categorical\n" +
                "     2 | Name=f2_suf, TrustyAI Type=number != Name=f2, TrustyAI Type=categorical\n" +
                "     3 | Name=f3_suf, TrustyAI Type=number != Name=f3, TrustyAI Type=categorical\n" +
                "     4 | Name=f4_suf, TrustyAI Type=number != Name=f4, TrustyAI Type=categorical\n" +
                "     5 | Name=f5_suf, TrustyAI Type=number != Name=f5, TrustyAI Type=categorical\n" +
                "     6 | Name=f6_suf, TrustyAI Type=number != Name=f6, TrustyAI Type=categorical\n" +
                "     7 | Name=f7_suf, TrustyAI Type=number != Name=f7, TrustyAI Type=categorical\n" +
                "     8 | Name=f8_suf, TrustyAI Type=number != Name=f8, TrustyAI Type=categorical\n" +
                "     9 | Name=f9_suf, TrustyAI Type=number != Name=f9, TrustyAI Type=categorical\n" +
                "================================================================================", tableAndWidth.getFirst());
    }

    @Test
    void featureListComparisonDifferentSingleIDX() {
        List<Feature> f1 = generateFeatures(10, "", Type.NUMBER, 0);
        List<Feature> f2 = generateFeatures(10, "", Type.NUMBER, 0);
        f2.set(5, new Feature("f5_other", Type.CATEGORICAL, new Value(5.1)));
        Pair<String, Integer> tableAndWidth = IOUtils.featureListComparison(f1, f2, "Feature List 1", "Feature List 2", true);
        assertEquals("=== Feature Mismatches ============================================================================================================\n" +
                " Index |                                        Feature List 1 !=                                                    Feature List 2\n" +
                "-----------------------------------------------------------------------------------------------------------------------------------\n" +
                "     5 | Name=f5, TrustyAI Type=number, Class=Integer, Value=5 != Name=f5_other, TrustyAI Type=categorical, Class=Double, Value=5.1\n" +
                "===================================================================================================================================", tableAndWidth.getFirst());
    }

    @Test
    void featureListComparisonDifferentLength() {
        List<Feature> f1 = generateFeatures(5, "", Type.NUMBER, 0);
        List<Feature> f2 = generateFeatures(10, "", Type.NUMBER, 0);
        Pair<String, Integer> tableAndWidth = IOUtils.featureListComparison(f1, f2, "Feature List 1", "Feature List 2", true);
        assertEquals("=== Feature Mismatches ============================================\n" +
                " Index | Feature List 1 !=                           Feature List 2\n" +
                "-------------------------------------------------------------------\n" +
                "     5 |            n/a != Feature{name='f5', type=number, value=5}\n" +
                "     6 |            n/a != Feature{name='f6', type=number, value=6}\n" +
                "     7 |            n/a != Feature{name='f7', type=number, value=7}\n" +
                "     8 |            n/a != Feature{name='f8', type=number, value=8}\n" +
                "     9 |            n/a != Feature{name='f9', type=number, value=9}\n" +
                "===================================================================", tableAndWidth.getFirst());
    }
}
