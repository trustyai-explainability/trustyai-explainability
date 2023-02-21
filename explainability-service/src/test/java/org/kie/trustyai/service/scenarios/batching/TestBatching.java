package org.kie.trustyai.service.scenarios.batching;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.parsers.CSVParser;
import org.kie.trustyai.service.data.storage.BatchReader;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(BatchingTestProfile.class)
class TestBatching {

    private static final String makeHeader(List<String> names) {
        return String.join(",", names.stream().map(name -> "\"" + name + "\"").collect(Collectors.toList()));
    }

    @Test
    void testFewerThanBatch() throws IOException {
        final int batchSize = 20;
        final int N = 10;
        final List<String> names = List.of("xa-1", "xa-2", "xa-3");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        final InputStream stream = batchStorage.getDataInputStream("inputs.csv");
        final List<String> lines = BatchReader.readEntries(stream, batchSize);
        System.out.println(lines);
        assertEquals(N + 1, lines.size());
        assertEquals(makeHeader(names), lines.get(0));
    }

    @Test
    void testBiggerThanBatch() throws IOException {
        final int batchSize = 20;
        final int N = 1000;
        final List<String> names = List.of("xa-1");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        final InputStream stream = batchStorage.getDataInputStream("inputs.csv");
        final List<String> lines = BatchReader.readEntries(stream, batchSize);
        System.out.println(lines);
        assertEquals(batchSize + 1, lines.size());
        assertEquals(makeHeader(names), lines.get(0));
    }

    @Test
    void testConvertToDataframe() throws IOException {
        final int batchSize = 20;
        final int N = 1000;
        final List<String> names = List.of("xa-1");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        final InputStream inStream = batchStorage.getDataInputStream("inputs.csv");
        final List<String> inLines = BatchReader.readEntries(inStream, batchSize);
        final ByteBuffer inputBuffer = ByteBuffer.wrap(BatchReader.linesToBytes(inLines));

        final InputStream outStream = batchStorage.getDataInputStream("outputs.csv");
        final List<String> outLines = BatchReader.readEntries(outStream, batchSize);
        final ByteBuffer outputBuffer = ByteBuffer.wrap(BatchReader.linesToBytes(outLines));

        final CSVParser parser = new CSVParser();
        final Dataframe dataframe = parser.toDataframe(inputBuffer, outputBuffer);

        assertEquals(batchSize, dataframe.getRowDimension());
        assertEquals(names, dataframe.getInputDataframe().getColumnNames());
        assertEquals(names, dataframe.getOutputDataframe().getColumnNames());
        assertEquals(2, dataframe.getColumnDimension());
    }

    @Test
    void testConvertToDataframeSmallerThanBatch() throws IOException {
        final int batchSize = 100;
        final int N = 90;
        final List<String> names = List.of("xa-1", "xa-2");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        final InputStream inStream = batchStorage.getDataInputStream("inputs.csv");
        final List<String> inLines = BatchReader.readEntries(inStream, batchSize);
        final ByteBuffer inputBuffer = ByteBuffer.wrap(BatchReader.linesToBytes(inLines));

        final InputStream outStream = batchStorage.getDataInputStream("outputs.csv");
        final List<String> outLines = BatchReader.readEntries(outStream, batchSize);
        final ByteBuffer outputBuffer = ByteBuffer.wrap(BatchReader.linesToBytes(outLines));

        final CSVParser parser = new CSVParser();
        final Dataframe dataframe = parser.toDataframe(inputBuffer, outputBuffer);

        assertEquals(N, dataframe.getRowDimension());
        assertEquals(names, dataframe.getInputDataframe().getColumnNames());
        assertEquals(names, dataframe.getOutputDataframe().getColumnNames());
        assertEquals(4, dataframe.getColumnDimension());
    }

    @Test
    void testConvertToDataframeNoHeader() {
        final int batchSize = 100;
        final int N = 90;
        final List<String> names = List.of("xa-1", "xa-2");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        batchStorage.setWithHeader(false);
        final InputStream inStream;
        try {
            inStream = batchStorage.getDataInputStream("inputs.csv");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        final List<String> inLines;
        try {
            inLines = BatchReader.readEntries(inStream, batchSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final ByteBuffer inputBuffer = ByteBuffer.wrap(BatchReader.linesToBytes(inLines));

        final InputStream outStream;
        try {
            outStream = batchStorage.getDataInputStream("outputs.csv");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        final List<String> outLines;
        try {
            outLines = BatchReader.readEntries(outStream, batchSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final ByteBuffer outputBuffer = ByteBuffer.wrap(BatchReader.linesToBytes(outLines));

        final CSVParser parser = new CSVParser();

        final Dataframe dataframe = parser.toDataframe(inputBuffer, outputBuffer);

        assertEquals(N - 1, dataframe.getRowDimension());
        assertEquals(4, dataframe.getColumnDimension());

    }
}
