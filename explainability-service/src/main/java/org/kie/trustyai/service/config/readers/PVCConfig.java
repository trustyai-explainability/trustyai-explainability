package org.kie.trustyai.service.config.readers;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "pvc", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface PVCConfig {

    Optional<String> inputFilename();

    Optional<String> outputFilename();

}
