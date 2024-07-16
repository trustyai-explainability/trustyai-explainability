package org.kie.trustyai.service.payloads.explainers;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.local.lime.LimeConfig;
import org.kie.trustyai.service.payloads.explainers.lime.LimeExplainerConfig;

import static org.junit.jupiter.api.Assertions.*;

class LimeExplainerConfigTest {

    @Test
    void testDefaultValues() {
        final LimeExplainerConfig config = new LimeExplainerConfig();

        assertEquals(LimeConfig.DEFAULT_NO_OF_SAMPLES, config.getnSamples());
        assertEquals(LimeConfig.DEFAULT_SEPARABLE_DATASET_RATIO, config.getSeparableDatasetRation());
        assertEquals(LimeConfig.DEFAULT_NO_OF_RETRIES, config.getRetries());
        assertEquals(LimeConfig.DEFAULT_ADAPT_DATASET_VARIANCE, config.isAdaptiveVariance());
        assertEquals(LimeConfig.DEFAULT_PENALIZE_BALANCE_SPARSE, config.isPenalizeBalanceSparse());
        assertEquals(LimeConfig.DEFAULT_PROXIMITY_FILTER, config.isProximityFilter());
        assertEquals(LimeConfig.DEFAULT_PROXIMITY_THRESHOLD, config.getProximityThreshold());
        assertEquals(LimeConfig.DEFAULT_PROXIMITY_KERNEL_WIDTH, config.getProximityKernelWidth());
        assertEquals(LimeConfig.DEFAULT_ENCODING_CLUSTER_THRESHOLD, config.getEncodingClusterThreshold());
        assertEquals(LimeConfig.DEFAULT_ENCODING_GAUSSIAN_FILTER_WIDTH, config.getEncodingGaussianFilterWidth());
        assertEquals(LimeConfig.DEFAULT_NORMALIZE_WEIGHTS, config.isNormalizeWeights());
        assertEquals(LimeConfig.DEFAULT_HIGH_SCORE_ZONES, config.isHighScoreFeatureZones());
        assertEquals(LimeConfig.DEFAULT_FEATURE_SELECTION, config.isFeatureSelection());
        assertEquals(LimeConfig.DEFAULT_NO_OF_FEATURES, config.getnFeatures());
        assertEquals(LimeConfig.DEFAULT_TRACK_COUNTERFACTUALS, config.isTrackCounterfactuals());
        assertEquals(LimeConfig.DEFAULT_USE_WLR_LINEAR_MODEL, config.isUseWLRModel());
        assertEquals(LimeConfig.DEFAULT_FILTER_INTERPRETABLE, config.isFilterInterpretable());
    }

    @Test
    void testCustomValues() {
        final LimeExplainerConfig config = new LimeExplainerConfig();
        config.setnSamples(500);
        config.setSeparableDatasetRation(0.8);
        config.setRetries(5);
        config.setAdaptiveVariance(false);
        config.setPenalizeBalanceSparse(false);
        config.setProximityFilter(false);
        config.setProximityThreshold(0.9);
        config.setProximityKernelWidth(0.6);
        config.setEncodingClusterThreshold(0.1);
        config.setEncodingGaussianFilterWidth(0.2);
        config.setNormalizeWeights(true);
        config.setHighScoreFeatureZones(false);
        config.setFeatureSelection(false);
        config.setnFeatures(10);
        config.setTrackCounterfactuals(true);
        config.setUseWLRModel(false);
        config.setFilterInterpretable(true);

        assertEquals(500, config.getnSamples());
        assertEquals(0.8, config.getSeparableDatasetRation());
        assertEquals(5, config.getRetries());
        assertFalse(config.isAdaptiveVariance());
        assertFalse(config.isPenalizeBalanceSparse());
        assertFalse(config.isProximityFilter());
        assertEquals(0.9, config.getProximityThreshold());
        assertEquals(0.6, config.getProximityKernelWidth());
        assertEquals(0.1, config.getEncodingClusterThreshold());
        assertEquals(0.2, config.getEncodingGaussianFilterWidth());
        assertTrue(config.isNormalizeWeights());
        assertFalse(config.isHighScoreFeatureZones());
        assertFalse(config.isFeatureSelection());
        assertEquals(10, config.getnFeatures());
        assertTrue(config.isTrackCounterfactuals());
        assertFalse(config.isUseWLRModel());
        assertTrue(config.isFilterInterpretable());
    }

    @Test
    void testInvalidValues() {
        final LimeExplainerConfig config = new LimeExplainerConfig();

        assertThrows(IllegalArgumentException.class, () -> {
            config.setnSamples(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            config.setRetries(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            config.setnFeatures(-1);
        });
    }
}
