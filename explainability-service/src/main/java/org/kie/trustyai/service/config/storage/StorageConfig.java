package org.kie.trustyai.service.config.storage;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "storage", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface StorageConfig {

    Optional<String> dataFilename();

    Optional<String> dataFolder();
}
