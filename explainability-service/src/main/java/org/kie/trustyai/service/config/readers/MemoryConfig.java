package org.kie.trustyai.service.config.readers;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "memory", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface MemoryConfig {

    Optional<String> inputFilename();

    Optional<String> outputFilename();

}
