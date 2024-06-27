package org.kie.trustyai.service.data.storage.hibernate.migration;

import org.kie.trustyai.service.profiles.hibernate.migration.silos.MigrationTestProfileLoadAndSave;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(MigrationTestProfileLoadAndSave.class)
class LoadAndSaveMigratedDataTest extends BaseMigrationTest {
    @Override
    void triggerMigration() {
    }
}
