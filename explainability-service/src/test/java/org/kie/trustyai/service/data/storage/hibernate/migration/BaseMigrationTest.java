package org.kie.trustyai.service.data.storage.hibernate.migration;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.flatfile.MockPVCStorage;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseMigrationTest {

    static String MODEL_NAME = "EXAMPLE_MODEL_";
    static int N_DFS = 10;
    static List<Dataframe> dfs = IntStream
            .range(0, N_DFS)
            .mapToObj(i -> DataframeGenerators.generatePositionalHintedDataframe(100 + i, i + 5))
            .collect(Collectors.toList());

    @Inject
    Instance<MockPVCStorage> oldStorage;

    @Inject
    Instance<MockHibernateStorage> storage;

    @Inject
    Instance<MockHibernateDatasource> datasource;

    @BeforeAll
    void setup() {
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

        for (int i = 0; i < N_DFS; i++) {
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-data.csv");
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-metadata.json");
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-internal_data.csv");
        }
    }

    // save data straight to CSV
    void populateOriginal() {
        for (int i = 0; i < N_DFS; i++) {
            oldStorage.get().emulateDatasourceSaveDataframe(dfs.get(i), MODEL_NAME + i, false);
        }
    }

    @AfterAll
    void clearStorage() {
        for (int i = 0; i < N_DFS; i++) {
            storage.get().clearData(MODEL_NAME + i);
        }
    }

    void validateRetrieval(MockHibernateDatasource datasource) {
        for (int i = 0; i < N_DFS; i++) {
            Dataframe original = dfs.get(i);
            Dataframe retrieved = datasource.getDataframe(MODEL_NAME + i, 1000);
            DataframeGenerators.roughEqualityCheck(original, retrieved);
        }
    }

    @Test
    void retrieveAndSaveOnMigratedDF() {
        triggerMigration();

        validateRetrieval(datasource.get());

        for (int i = 0; i < N_DFS; i++) {
            int dfLen = 100 + i;
            Dataframe newDF = DataframeGenerators.generatePositionalHintedDataframe(dfLen, i + 5);
            datasource.get().saveDataframe(newDF, MODEL_NAME + i);
            Dataframe original = dfs.get(i);

            // retrieve migrated DF
            Dataframe retrievedFirst = datasource.get().getDataframe(MODEL_NAME + i, 0, dfLen);

            // retrieve df saved after migrated df
            Dataframe retrievedSecond = datasource.get().getDataframe(MODEL_NAME + i, dfLen, dfLen + dfLen);
            DataframeGenerators.roughEqualityCheck(original, retrievedFirst);
            DataframeGenerators.roughEqualityCheck(newDF, retrievedSecond);
        }
    }

    // trigger the migration
    abstract void triggerMigration();
}
