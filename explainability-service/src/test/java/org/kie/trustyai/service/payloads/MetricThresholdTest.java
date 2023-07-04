package org.kie.trustyai.service.payloads;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricThresholdTest {

    @Test
    void testSimpleThresholds() {
        MetricThreshold mt = new MetricThreshold(-10.0, 10.0, 0.0);
        assertFalse(mt.outsideBounds);
        mt = new MetricThreshold(-10.0, 10.0, -11.0);
        assertTrue(mt.outsideBounds);
        mt = new MetricThreshold(-10.0, 10.0, 12.0);
        assertTrue(mt.outsideBounds);
        mt = new MetricThreshold(-10.0, 10.0, -10.0);
        assertFalse(mt.outsideBounds);
        mt = new MetricThreshold(-10.0, 10.0, 10.0);
        assertFalse(mt.outsideBounds);

    }

}