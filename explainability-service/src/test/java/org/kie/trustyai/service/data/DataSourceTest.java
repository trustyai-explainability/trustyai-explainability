package org.kie.trustyai.service.data;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.data.parsers.CSVParser;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.mocks.MockDatasource;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
class DataSourceTest {

    @Inject
    Instance<Storage> storage;

    @Test
    void testSavingAndReadingDataframe() {
        DataSource dataSource = new DataSource();
        dataSource.storage = storage;
        dataSource.parser = new CSVParser();
        Dataframe dataframe = new MockDatasource().generateRandomDataframe(10);
        dataSource.saveDataframe(dataframe, "fake-model");
        Dataframe readDataframe = dataSource.getDataframe("fake-model");
        assertNotNull(readDataframe);
        assertEquals(10, dataframe.getRowDimension());
        for (int i = 0; i < dataframe.getRowDimension(); i++) {
            assertEquals(dataframe.getRow(i), readDataframe.getRow(i));
        }
    }
}
