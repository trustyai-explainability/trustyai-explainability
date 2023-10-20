package org.kie.trustyai.service.data.utils;

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;
import org.kie.trustyai.service.payloads.values.DataType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
class MetadataUtilsTest {
    @Inject
    Instance<MockDatasource> datasource;

    @Inject
    Instance<MockMemoryStorage> storage;

    @Test
    public void testLargeDataFrameSchema() {
        int ncols = 500_000;
        final Dataframe dataframe = datasource.get().generateRandomNColumnDataframe(1, ncols);
        List<DataType> dataTypeList = Collections.nCopies(ncols, DataType.INT32);
        assertEquals(MetadataUtils.getInputSchema(dataframe), MetadataUtils.getInputSchema(dataframe, dataTypeList));
    }
}
