package org.kie.trustyai.service.mocks.hibernate;

import java.util.Optional;

import org.kie.trustyai.service.config.storage.MigrationConfig;

public class MockMigrationConfigFactory {
    private static class MockMigrationConfig implements MigrationConfig {
        @Override
        public Optional<String> fromFolder() {
            return Optional.of("/tmp");
        }

        @Override
        public Optional<String> fromFilename() {
            return Optional.of("data.csv");
        }
    }

    private static class EmptyMigrationConfig implements MigrationConfig {
        @Override
        public Optional<String> fromFolder() {
            return Optional.empty();
        }

        @Override
        public Optional<String> fromFilename() {
            return Optional.empty();
        }
    }

    public static EmptyMigrationConfig nonMigrating() {
        return new EmptyMigrationConfig();
    }

    public static MockMigrationConfig migrating() {
        return new MockMigrationConfig();
    }
}
