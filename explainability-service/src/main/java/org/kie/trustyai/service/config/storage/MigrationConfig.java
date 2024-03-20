package org.kie.trustyai.service.config.storage;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "migration", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface MigrationConfig {
    @WithDefault("")
    Optional<String> fromFolder();

    @WithDefault("")
    Optional<String> fromFilename();
}
