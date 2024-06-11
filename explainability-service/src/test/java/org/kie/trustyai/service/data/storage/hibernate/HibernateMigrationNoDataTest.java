package org.kie.trustyai.service.data.storage.hibernate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.profiles.hibernate.InvalidMigrationTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(InvalidMigrationTestProfile.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateMigrationNoDataTest {
    @Inject
    Instance<MockHibernateDatasource> datasource;

    String MODEL_NAME = "EXAMPLE_MODEL_";

    // verify that pointing the migration config at a non-existant directory does not cause issues
    @Test
    void saveDF() {
        Dataframe newDF = DataframeGenerators.generatePositionalHintedDataframe(100, 15);
        datasource.get().saveDataframe(newDF, MODEL_NAME);
        Dataframe retrievedDF = datasource.get().getDataframe(MODEL_NAME);
        DataframeGenerators.roughEqualityCheck(newDF, retrievedDF);

    }
}
