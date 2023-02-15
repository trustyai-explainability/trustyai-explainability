package org.kie.trustyai.service.config.readers;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "pvc", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface PVCConfig {

    String inputFilename();

    String outputFilename();

}
