package org.kie.trustyai.service.data.storage.hibernate.migration;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.migration.scenarios.MigrationTestProfilePreviousDB;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestProfile(MigrationTestProfilePreviousDB.class)
class PreviousDBMigrationTest {

    private static final Logger LOG = Logger.getLogger(PreviousDBMigrationTest.class);

    static String MODEL_NAME = "EXAMPLE_MODEL";
    static Dataframe df = DataframeGenerators.generateRandomNColumnDataframeMatchingKservePayloads(5000, 5);
    int timesRan = 0;

    @Inject
    Instance<MockHibernateStorage> storage;

    @Inject
    Instance<MockHibernateDatasource> datasource;

    void save() {
        datasource.get().saveDataframe(df, MODEL_NAME);
    }

    @AfterEach
    @BeforeEach
    void clearStorage() {
        storage.get().clearData(MODEL_NAME);
    }

    /*
     * This function will be run twice, by PreviousDBMigration0Test and PreviousDBMigration1Test in some arbitrary order.
     * 
     * The first time this test is run, no previous data will exist in the DB, and it will 5,000 rows will be saved to the DB.
     * The second time the test is run, existing data will appear. An additional 5,000 rows will be saved, and the assertion
     * will check that the previous data from the existing DB has been successfully migrated.
     */

    @Test
    void testPreviousDBMigration() throws InterruptedException {
        long numObs = 0L;
        for (int timesRan = 0; timesRan < 2; timesRan++) {
            LOG.info("=== RUN " + timesRan + " ===");
            datasource.destroy(datasource.get());

            long initObs = datasource.get().getNumObservations(MODEL_NAME);
            long expectedObs = (long) (timesRan * df.getRowDimension());
            assertEquals(expectedObs, initObs);
            save();

            numObs = datasource.get().getNumObservations(MODEL_NAME);

            if (timesRan == 0) {
                assertEquals(df.getRowDimension(), numObs);
                assertFalse(datasource.get().isMigratedFromPreviousDB());
            } else {
                assertTrue(datasource.get().isMigratedFromPreviousDB());
                assertEquals(df.getRowDimension() * 2L, numObs);
                clearStorage();
            }
        }

    }
}
