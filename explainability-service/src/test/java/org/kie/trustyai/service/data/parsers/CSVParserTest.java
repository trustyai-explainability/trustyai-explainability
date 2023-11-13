package org.kie.trustyai.service.data.parsers;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.mocks.MockDatasource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CSVParserTest {

    @Test
    void testRestoringDataframeFromByteBuffers() {
        CSVParser csvParser = new CSVParser();
        Dataframe dataframe = new MockDatasource().generateRandomDataframe(10);
        ByteBuffer[] byteBuffers = csvParser.toByteBuffers(dataframe, false);
        assertNotNull(byteBuffers);
        assertEquals(2, byteBuffers.length);
        Metadata metadata = new Metadata();
        metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
        metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
        metadata.setObservations(dataframe.getRowDimension());
        Dataframe restoredDataframe = csvParser.toDataframe(byteBuffers[0], byteBuffers[1], metadata);
        assertNotNull(restoredDataframe);
        assertEquals(10, dataframe.getRowDimension());
        for (int i = 0; i < dataframe.getRowDimension(); i++) {
            assertEquals(dataframe.getRow(i), restoredDataframe.getRow(i));
        }
    }

    @Test
    void testRestoringLargeDataframeFromByteBuffers() {
        // tests to make sure runtime is reasonable for very large-columned dfs

        CSVParser csvParser = new CSVParser();
        Dataframe dataframe = new MockDatasource().generateRandomNColumnDataframe(10, 100_000);
        ByteBuffer[] byteBuffers = csvParser.toByteBuffers(dataframe, false);
        assertNotNull(byteBuffers);
        assertEquals(2, byteBuffers.length);
        Metadata metadata = new Metadata();
        metadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
        metadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
        metadata.setObservations(dataframe.getRowDimension());
        Dataframe restoredDataframe = csvParser.toDataframe(byteBuffers[0], byteBuffers[1], metadata);
        assertNotNull(restoredDataframe);
        assertEquals(10, dataframe.getRowDimension());
        for (int i = 0; i < dataframe.getRowDimension(); i++) {
            assertEquals(dataframe.getRow(i), restoredDataframe.getRow(i));
        }
    }
}