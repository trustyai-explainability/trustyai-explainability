package org.kie.trustyai.service.mocks.hibernate;

import org.kie.trustyai.service.config.storage.MigrationConfig;

import java.util.Optional;

public class MockMigrationConfig implements MigrationConfig {

    // not needed
    @Override
    public Optional<String> fromFolder() {
        return Optional.of("/tmp");
    }

    @Override
    public Optional<String>  fromFilename() {
        return Optional.of("data.csv");
    }
}
