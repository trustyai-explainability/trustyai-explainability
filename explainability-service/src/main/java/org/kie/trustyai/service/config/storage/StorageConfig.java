package org.kie.trustyai.service.config.storage;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "storage", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface StorageConfig {

    String dataFilename();

    String dataFolder();
}
