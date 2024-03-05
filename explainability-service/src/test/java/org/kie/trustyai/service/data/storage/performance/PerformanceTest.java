package org.kie.trustyai.service.data.storage.performance;

import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import jakarta.enterprise.inject.Instance;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class PerformanceTest<T extends Storage> {
    String MODEL_ID = "example_model";

    @PersistenceContext
    EntityManager em;

    Instance<T> storage;

    int N_ROWS = 5_000;
    int N_DF_COPIES = 30;
    int N_COLS = 25;
    int BATCH_SIZE = 1000;
    int N_TESTS = 50;
    int N_WARMUP = 3;
    Random rn = new Random(0L);

    public int[] randomBatchSizes = IntStream.range(0, N_TESTS).map(i -> rn.nextInt(BATCH_SIZE - 10) + 10).toArray();

    Dataframe last = generateDF();

    public Dataframe generateDF() {
        return DataframeGenerators.generatePositionalHintedDataframe(N_ROWS, N_COLS);
    }

    void test(Consumer<Integer> testFunc) {
        long warmStart = System.currentTimeMillis();
        for (int i = 0; i < N_WARMUP; i++) {
            testFunc.accept(i);
        }
        long start = System.currentTimeMillis();
        System.out.println("warmup finished after " + (start - warmStart) / 1000. + "s");

        for (int i = 0; i < N_TESTS; i++) {
            testFunc.accept(i);
        }
        System.out.println("main test: " + (System.currentTimeMillis() - start) / 1000. + "s");
    }
}
