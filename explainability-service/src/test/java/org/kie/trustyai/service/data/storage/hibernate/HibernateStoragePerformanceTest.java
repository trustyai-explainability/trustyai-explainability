package org.kie.trustyai.service.data.storage.hibernate;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateStoragePerformanceTest {
    @Inject
    Instance<MockHibernateStorage> storage;

    @PersistenceContext
    EntityManager em;

    String MODEL_ID = "example_model";

    @Transactional
    @BeforeAll
    void emptyStorage() {
        em.clear();
        storage.get().save(original, MODEL_ID);
    }

    int N_ROWS = 1_000;
    int BATCH_SIZE = 50;

    Dataframe original = DataframeGenerators.generatePositionalHintedDataframe(N_ROWS, 25);
    Dataframe batched = original.filterByRowIndex(IntStream.range(N_ROWS-BATCH_SIZE, N_ROWS).boxed().collect(Collectors.toList()));

    @ParameterizedTest
    @ValueSource(ints={1, 2, 3})
    void testBatchedRead(int ignored) {
        Dataframe recovered = storage.get().readData(MODEL_ID, BATCH_SIZE);
        DataframeGenerators.roughEqualityCheck(batched, recovered);
    }

    @ParameterizedTest
    @ValueSource(ints={1, 2, 3})
    void testBatchedReadManual(int ignored) {
        Dataframe recovered = storage.get().readAllData(MODEL_ID);
        Dataframe recovered2 = recovered.filterByRowIndex(IntStream.range(N_ROWS-BATCH_SIZE, N_ROWS).boxed().collect(Collectors.toList()));
        DataframeGenerators.roughEqualityCheck(batched, recovered2);
    }

}
