package org.kie.trustyai.service.data.datasources;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.parsers.CSVParser;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;
import org.kie.trustyai.service.utils.DataframeGenerators;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestProfile(MemoryTestProfile.class)
class DataSourceTest {

    @Inject
    Instance<Storage<?, ?>> storage;

    @Inject
    Instance<DataSource> dataSourceInstance;

    @Test
    void testSavingAndReadingDataframe() {
        DataSource dataSource = dataSourceInstance.get();
        dataSource.storage = storage;
        dataSource.parser = new CSVParser();
        Dataframe dataframe = DataframeGenerators.generateRandomDataframe(10);
        dataSource.saveDataframe(dataframe, "fake-model");
        Dataframe readDataframe = dataSource.getDataframe("fake-model");
        assertNotNull(readDataframe);
        assertEquals(10, dataframe.getRowDimension());
        for (int i = 0; i < dataframe.getRowDimension(); i++) {
            assertEquals(dataframe.getRow(i), readDataframe.getRow(i));
        }
    }
}
