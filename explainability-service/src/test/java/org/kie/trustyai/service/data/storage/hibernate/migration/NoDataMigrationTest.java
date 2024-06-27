package org.kie.trustyai.service.data.storage.hibernate.migration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.profiles.hibernate.InvalidMigrationTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(InvalidMigrationTestProfile.class)
class NoDataMigrationTest extends BaseMigrationTest {
    @BeforeAll
    @Override
    void setup() {
        clearOriginal();
        // do not populate old storage
    }

    @Override
    void triggerMigration() {
    }

    // verify that pointing the migration config at a non-existant directory does not cause issues
    @Override
    @Test
    void retrieveAndSaveOnMigratedDF() {
        Dataframe newDF = DataframeGenerators.generatePositionalHintedDataframe(100, 15);
        datasource.get().saveDataframe(newDF, MODEL_NAME);
        Dataframe retrievedDF = datasource.get().getDataframe(MODEL_NAME);
        DataframeGenerators.roughEqualityCheck(newDF, retrievedDF);
    }
}
