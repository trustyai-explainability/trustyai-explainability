package org.kie.trustyai.service.config.storage;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.util.Optional;

@ConfigMapping(prefix = "migration", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface MigrationConfig {
    @WithDefault("")
    Optional<String> fromFolder();

    @WithDefault("")
    Optional<String> fromFilename();
}
