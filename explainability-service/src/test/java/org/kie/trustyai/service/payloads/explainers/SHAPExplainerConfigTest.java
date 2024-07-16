package org.kie.trustyai.service.payloads.explainers;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.local.shap.ShapConfig;
import org.kie.trustyai.service.payloads.explainers.shap.SHAPExplainerConfig;

import static org.junit.jupiter.api.Assertions.*;

public class SHAPExplainerConfigTest {
    @Test
    void testDefaultValues() {
        SHAPExplainerConfig config = new SHAPExplainerConfig();

        assertEquals(100, config.getnSamples());
        assertEquals(ShapConfig.LinkType.IDENTITY, config.getLinkType());
        assertEquals(ShapConfig.RegularizerType.AUTO, config.getRegularizer());
        assertEquals(0.95, config.getConfidence());
        assertFalse(config.isTrackCounterfactuals());
    }

    @Test
    void testCustomValues() {
        final SHAPExplainerConfig config = new SHAPExplainerConfig();
        config.setnSamples(500);
        config.setLinkType(ShapConfig.LinkType.LOGIT);
        config.setRegularizer(ShapConfig.RegularizerType.AIC);
        config.setConfidence(0.99);
        config.setTrackCounterfactuals(true);

        assertEquals(500, config.getnSamples());
        assertEquals(ShapConfig.LinkType.LOGIT, config.getLinkType());
        assertEquals(ShapConfig.RegularizerType.AIC, config.getRegularizer());
        assertEquals(0.99, config.getConfidence());
        assertTrue(config.isTrackCounterfactuals());
    }

    @Test
    void testInvalidValues() {
        final SHAPExplainerConfig config = new SHAPExplainerConfig();

        assertThrows(IllegalArgumentException.class, () -> {
            config.setnSamples(-1);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            config.setConfidence(-0.1);
        });

    }
}
