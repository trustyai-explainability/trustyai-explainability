package org.kie.trustyai.service.data.storage.hibernate;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.pvc.MockPVCStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@QuarkusTest
@TestProfile(MigrationTestProfile.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateMigratorTest {
    @Inject
    Instance<MockPVCStorage> oldStorage;

    @Inject
    Instance<MockHibernateDatasource> datasource;

    String MODEL_NAME = "EXAMPLE_MODEL_";

    int N_DFS = 10;
    List<Dataframe> dfs = IntStream
            .range(0, N_DFS)
            .mapToObj(i -> DataframeGenerators.generateRandomNColumnDataframe(100 + i, i + 5))
            .collect(Collectors.toList());

    @BeforeAll
    void populateOriginal() {
        datasource.get().setStorageOverride(oldStorage.get());
        for (int i = 0; i < N_DFS; i++) {
            datasource.get().saveDataframe(dfs.get(i), MODEL_NAME + i);
        }
        datasource.get().clearStorageOverride();
    }

    @Test
    void retrieveMigratedDF(){
        for (int i=0; i<N_DFS; i++){
            DataframeGenerators.roughEqualityCheck(dfs.get(i), datasource.get().getDataframe(MODEL_NAME+i));
        }
    }
}