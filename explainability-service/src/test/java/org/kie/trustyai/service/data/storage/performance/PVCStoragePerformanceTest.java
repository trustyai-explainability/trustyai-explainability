package org.kie.trustyai.service.data.storage.performance;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.mocks.flatfile.MockCSVDatasource;
import org.kie.trustyai.service.profiles.flatfile.PVCTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(PVCTestProfile.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
class PVCStoragePerformanceTest extends PerformanceTest {

    @Inject
    Instance<MockCSVDatasource> datasource;
    private final static String MODEL_ID = "example-model";
    private final static String FILENAME = "file.txt";

    @BeforeAll
    void populateStorage() throws JsonProcessingException {
        datasource.get().reset();
        datasource.get().saveDataframe(generateDF(), MODEL_ID);

        for (int i = 0; i < N_DF_COPIES; i++) {
            datasource.get().saveDataframe(generateDF(), MODEL_ID);
        }

        datasource.get().saveDataframe(last, MODEL_ID);
    }

    @Test
    void testBatchedRead() {
        test((i) -> {
            Dataframe batched = last.filterByRowIndex(IntStream.range(N_ROWS - randomBatchSizes[(int) i], N_ROWS).boxed().collect(Collectors.toList()));
            Dataframe recovered = datasource.get().getDataframe(MODEL_ID, randomBatchSizes[(int) i]);
            DataframeGenerators.roughValueEqualityCheck(batched, recovered);
        });
    }
}
