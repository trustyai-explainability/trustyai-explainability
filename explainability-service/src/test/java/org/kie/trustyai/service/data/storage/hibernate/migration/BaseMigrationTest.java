package org.kie.trustyai.service.data.storage.hibernate.migration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.datasources.HibernateDataSource;
import org.kie.trustyai.service.mocks.flatfile.MockPVCStorage;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseMigrationTest {
    private static final Logger LOG = Logger.getLogger(BaseMigrationTest.class);
    static String MODEL_NAME = "EXAMPLE_MODEL_";
    int n_dfs;
    List<Dataframe> dfs;

    @Inject
    Instance<MockPVCStorage> oldStorage;

    @Inject
    Instance<MockHibernateStorage> storage;

    @Inject
    Instance<MockHibernateDatasource> datasource;

    List<Dataframe> getDFs() {
        return IntStream
                .range(0, 10)
                .mapToObj(i -> DataframeGenerators.generatePositionalHintedDataframe(500 + i, i + 5))
                .collect(Collectors.toList());
    }

    @BeforeAll
    void setup() throws InterruptedException {
        dfs = getDFs();
        n_dfs = dfs.size();
        clearOriginal();
        populateOriginal();
    }

    void clearOriginal() {
        oldStorage.get().emptyStorage("/tmp/" + "example-model" + "-data.csv");
        oldStorage.get().emptyStorage("/tmp/" + "example-model" + "-metadata.json");
        oldStorage.get().emptyStorage("/tmp/" + "example-model" + "-internal_data.csv");

        oldStorage.get().emptyStorage("/tmp/" + "example1" + "-data.csv");
        oldStorage.get().emptyStorage("/tmp/" + "example1" + "-metadata.json");
        oldStorage.get().emptyStorage("/tmp/" + "example1" + "-internal_data.csv");

        for (int i = 0; i < n_dfs; i++) {
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-data.csv");
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-metadata.json");
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-internal_data.csv");
        }
    }

    // save data straight to CSV
    void populateOriginal() throws InterruptedException {
        LOG.info("Populating Original Storage");
        for (int i = 0; i < n_dfs; i++) {
            oldStorage.get().emulateDatasourceSaveDataframe(dfs.get(i), MODEL_NAME + i, false);
        }
        while (oldStorage.get().listAllModelIds().size() < n_dfs) {
            LOG.info("Waiting for old datasource to populate");
            Thread.sleep(1000);
        }
    }

    @AfterAll
    void clearStorage() {
        for (int i = 0; i < n_dfs; i++) {
            storage.get().clearData(MODEL_NAME + i);
        }
        clearOriginal();
    }

    void waitForMigration() throws InterruptedException {
        boolean hasWaitWarned = false;
        HibernateDataSource ds = datasource.get();
        while (ds.isMigrationInProgress()) {
            if (!hasWaitWarned) {
                LOG.info("Migration test is waiting for migration to finish");
                hasWaitWarned = true;
            }
            Thread.sleep(500);
        }
        LOG.info("Migration is complete");
    }

    void validateRetrieval(MockHibernateDatasource datasource) throws InterruptedException {
        waitForMigration();
        for (int i = 0; i < n_dfs; i++) {
            Dataframe original = dfs.get(i);
            Dataframe retrieved = datasource.getDataframe(MODEL_NAME + i, original.getRowDimension());
            DataframeGenerators.roughEqualityCheck(original, retrieved);
        }
        LOG.info("Retrieval verified");
    }

    @Test
    void retrieveAndSaveOnMigratedDF() throws InterruptedException {
        validateRetrieval(datasource.get());

        LOG.info("Saving dfs");
        for (int i = 0; i < n_dfs; i++) {
            Dataframe original = dfs.get(i);
            int dfLen = original.getRowDimension();
            int toAdd = 100;
            Dataframe newDF = DataframeGenerators.generatePositionalHintedDataframe(toAdd, i + 5);
            datasource.get().saveDataframe(newDF, MODEL_NAME + i);

            // retrieve migrated DF
            Dataframe retrievedFirst = datasource.get().getDataframe(MODEL_NAME + i, 0, dfLen);

            // retrieve df saved after migrated df
            Dataframe retrievedSecond = datasource.get().getDataframe(MODEL_NAME + i, dfLen, dfLen + toAdd);
            DataframeGenerators.roughEqualityCheck(original, retrievedFirst);
            DataframeGenerators.roughEqualityCheck(newDF, retrievedSecond);
        }
    }
}
