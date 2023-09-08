package org.kie.trustyai.service.config.metrics;

import org.kie.trustyai.metrics.drift.meanshift.Meanshift;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "metrics", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface MetricsConfig {

    Spd spd();

    Dir dir();

    Drift drift();

    interface Spd {

        @WithDefault("-0.1")
        double thresholdLower();

        @WithDefault("0.1")
        double thresholdUpper();
    }

    interface Dir {

        @WithDefault("0.8")
        double thresholdLower();

        @WithDefault("1.2")
        double thresholdUpper();
    }

    interface Drift {
        @WithDefault(".05")
        double thresholdDelta();
    }

}
