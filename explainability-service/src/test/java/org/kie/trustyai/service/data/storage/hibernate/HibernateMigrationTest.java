package org.kie.trustyai.service.data.storage.hibernate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.storage.hibernate.profiles.MigrationTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.pvc.MockPVCStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(MigrationTestProfile.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateMigrationTest {
    @Inject
    Instance<MockPVCStorage> oldStorage;

    @Inject
    Instance<MockDatasource> datasource;

    String MODEL_NAME = "EXAMPLE_MODEL_";

    int N_DFS = 10;
    List<Dataframe> dfs = IntStream
            .range(0, N_DFS)
            .mapToObj(i -> DataframeGenerators.generatePositionalHintedDataframe(100 + i, i + 5))
            .collect(Collectors.toList());

    @BeforeAll
    void populateOriginal() {
        datasource.get().setStorageOverride(oldStorage.get());
        for (int i = 0; i < N_DFS; i++) {
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-data.csv");
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-metadata.json");
            oldStorage.get().emptyStorage("/tmp/" + MODEL_NAME + i + "-internal_data.csv");
            datasource.get().saveDataframe(dfs.get(i), MODEL_NAME + i);
        }

        datasource.get().clearStorageOverride();
    }

    @Test
    void retrieveMigratedDF() {
        for (int i = 0; i < N_DFS; i++) {
            Dataframe original = dfs.get(i);
            Dataframe retrieved = datasource.get().getDataframe(MODEL_NAME + i);
            DataframeGenerators.roughEqualityCheck(original, retrieved);
        }
    }

    @Test
    void saveOnTopOfMigratedDF() {
        for (int i = 0; i < N_DFS; i++) {
            int dfLen = 100 + i;
            Dataframe newDF = DataframeGenerators.generatePositionalHintedDataframe(dfLen, i + 5);
            datasource.get().saveDataframe(newDF, MODEL_NAME + i);
            Dataframe original = dfs.get(i);

            // retrieve migrated DF
            Dataframe retrievedFirst = datasource.get().getDataframe(MODEL_NAME + i, 0, dfLen);

            // retrieve normally saved DF after migration
            Dataframe retrievedSecond = datasource.get().getDataframe(MODEL_NAME + i, dfLen, dfLen + dfLen);
            DataframeGenerators.roughEqualityCheck(original, retrievedFirst);
            DataframeGenerators.roughEqualityCheck(newDF, retrievedSecond);
        }
    }
}
