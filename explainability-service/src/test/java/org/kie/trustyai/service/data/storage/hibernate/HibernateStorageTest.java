package org.kie.trustyai.service.data.storage.hibernate;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestProfile(HibernateTestProfile.class)
@QuarkusTestResource(H2DatabaseTestResource.class)
class HibernateStorageTest {
    @Inject
    Instance<MockHibernateStorage> storage;

    @PersistenceContext
    EntityManager em;

    String MODEL_ID = "example_model";

    @Transactional
    @BeforeEach
    void emptyStorage() {
        em.clear();
    }

    @Test
    void testWrite() {
        Dataframe df = DataframeGenerators.generateRandomNColumnDataframe(100, 10);
        storage.get().save(df, MODEL_ID);
        assertTrue(storage.get().dataframeExists(MODEL_ID));
    }

    @Test
    void testRead() {
        int nrows = 100;
        int ncols = 10;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        storage.get().save(original, MODEL_ID);
        Dataframe recovered = storage.get().readData(MODEL_ID, nrows);
        DataframeGenerators.roughEqualityCheck(original, recovered);
    }

    @Test
    void testBatchedRead() {
        int nrows = 100;
        int ncols = 10;
        int batch = 10;

        Dataframe original = DataframeGenerators.generateRandomNColumnDataframe(nrows, ncols);
        Dataframe batched = original.filterByRowIndex(IntStream.range(nrows-batch, nrows).boxed().collect(Collectors.toList()));
        storage.get().save(original, MODEL_ID);
        Dataframe recovered = storage.get().readData(MODEL_ID, batch);
        DataframeGenerators.roughEqualityCheck(batched, recovered);
    }




}
