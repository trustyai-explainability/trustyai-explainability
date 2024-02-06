package org.kie.trustyai.service.data.storage.flatflle;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.parsers.CSVParser;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.data.storage.hibernate.HibernateTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.pvc.MockPVCStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(PVCTestProfile.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PVCStoragePerformanceTest {

    @Inject
    Instance<MockDatasource> datasource;
    private final static String MODEL_ID = "example-model";
    private final static String FILENAME = "file.txt";

    @BeforeAll
    void populateStorage() throws JsonProcessingException {
        datasource.get().reset();
        datasource.get().saveDataframe(original, MODEL_ID);
    }

    int N_ROWS = 50_000;
    int BATCH_SIZE = 5000;

    Dataframe original = DataframeGenerators.generatePositionalHintedDataframe(N_ROWS, 25);
    Dataframe batched = original.filterByRowIndex(IntStream.range(N_ROWS-BATCH_SIZE, N_ROWS).boxed().collect(Collectors.toList()));


    @ParameterizedTest
    @ValueSource(ints={1, 2, 3})
    void testBatchedRead(int ignored) {
        Dataframe recovered = datasource.get().getDataframe(MODEL_ID, BATCH_SIZE);
        DataframeGenerators.roughValueEqualityCheck(batched, recovered);
    }

    @ParameterizedTest
    @ValueSource(ints={1, 2, 3})
    void testBatchedReadManuall(int ignored) {
        Dataframe recovered = datasource.get().getDataframe(MODEL_ID, N_ROWS);
        Dataframe recovered2 = recovered.filterByRowIndex(IntStream.range(N_ROWS-BATCH_SIZE, N_ROWS).boxed().collect(Collectors.toList()));
        DataframeGenerators.roughValueEqualityCheck(batched, recovered2);
    }

}
