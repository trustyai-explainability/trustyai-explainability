package org.kie.trustyai.service.data.storage.performance;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.storage.hibernate.HibernateTestProfile;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
//@QuarkusTestResource(MariaDatabaseTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HibernateStoragePerformanceTest extends PerformanceTest {
    @Inject
    Instance<MockHibernateStorage> storage;

    @BeforeAll
    void emptyStorage() {
        storage.get().save(generateDF(), MODEL_ID);

        for (int i = 0; i < N_DF_COPIES; i++) {
            storage.get().append(generateDF(), MODEL_ID);
            System.out.println(i);
        }

        storage.get().append(last, MODEL_ID);
    }

    @Test
    void testBatchedRead() {
        test((i) -> {
            Dataframe batched = last.filterByRowIndex(IntStream.range(N_ROWS - randomBatchSizes[(int) i], N_ROWS).boxed().collect(Collectors.toList()));
            Dataframe df = storage.get().readData(MODEL_ID, randomBatchSizes[(int) i]);
            DataframeGenerators.roughEqualityCheck(batched, df);

        });
    }
    //   @Test
    //    void testBatchedReadManual() {
    //        Consumer<Integer> testFunc = (i) -> {
    //            //Dataframe batched = original.filterByRowIndex(IntStream.range(N_ROWS - randomBatchSizes[i], N_ROWS).boxed().collect(Collectors.toList()));
    //            Dataframe recovered = storage.get().readAllData(MODEL_ID);
    //            Dataframe df = recovered.filterByRowIndex(IntStream.range(N_ROWS - randomBatchSizes[i], N_ROWS).boxed().collect(Collectors.toList()));
    //            //DataframeGenerators.roughEqualityCheck(batched, df);
    //            //em.clear();
    //        };
    //
    //        test(testFunc);
    //    }
    //

}
