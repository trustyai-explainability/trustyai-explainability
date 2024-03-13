package org.kie.trustyai.service.mocks.hibernate;

import org.kie.trustyai.service.config.storage.MigrationConfig;

import java.util.Optional;

public class MockEmptyMigrationConfig implements MigrationConfig {
    @Override
    public Optional<String> fromFolder() {
        return Optional.empty();
    }

    @Override
    public Optional<String>  fromFilename() {
        return Optional.empty();
    }
}
