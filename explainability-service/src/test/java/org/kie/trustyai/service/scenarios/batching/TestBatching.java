package org.kie.trustyai.service.scenarios.batching;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.parsers.CSVParser;
import org.kie.trustyai.service.data.storage.BatchReader;
import org.kie.trustyai.service.payloads.service.Schema;
import org.kie.trustyai.service.payloads.service.SchemaItem;
import org.kie.trustyai.service.payloads.values.DataType;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestProfile(BatchingTestProfile.class)
class TestBatching {

    private static final String makeHeader(List<String> names) {
        return String.join(",", names.stream().map(name -> "\"" + name + "\"").collect(Collectors.toList()));
    }

    private static Metadata createMetadata(List<String> inputNames, List<String> outputNames) {
        final Metadata metadata = new Metadata();

        final int s = inputNames.size();

        List<SchemaItem> inputSchema = IntStream.range(0, s).mapToObj(i -> new SchemaItem(DataType.DOUBLE, inputNames.get(i), i)).collect(Collectors.toList());
        List<SchemaItem> outputSchema = IntStream.range(s, s + outputNames.size()).mapToObj(i -> new SchemaItem(DataType.DOUBLE, outputNames.get(i - s), i)).collect(Collectors.toList());

        metadata.setInputSchema(Schema.from(inputSchema));
        metadata.setOutputSchema(Schema.from(outputSchema));

        return metadata;
    }

    @Test
    void testFewerThanBatch() throws IOException {
        final int batchSize = 20;
        final int N = 10;
        final List<String> names = List.of("xa-1", "xa-2", "xa-3");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        final InputStream stream = batchStorage.getDataStream();
        final List<String> lines = BatchReader.readEntries(stream, batchSize);
        assertEquals(N + 1, lines.size());
        assertEquals(makeHeader(names), lines.get(0));
        final List<Integer> expected = IntStream.range(0, N).boxed().collect(Collectors.toList());
        final List<Integer> actual = IntStream.range(1, N + 1).mapToObj(i -> (int) Math.floor(Double.parseDouble(lines.get(i).split(",")[0]))).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void testBiggerThanBatch() throws IOException {
        final int batchSize = 20;
        final int N = 1000;
        final List<String> names = List.of("xa-1");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        batchStorage.setWithHeader(false);
        final InputStream stream = batchStorage.getDataStream();
        final List<String> lines = BatchReader.readEntries(stream, batchSize);
        assertEquals(batchSize, lines.size());
        final List<Integer> expected = IntStream.range(N - batchSize, N).boxed().collect(Collectors.toList());
        final List<Integer> actual = IntStream.range(0, batchSize).mapToObj(i -> (int) Math.floor(Double.parseDouble(lines.get(i).split(",")[0]))).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void testConvertToDataframe() throws IOException {
        final int batchSize = 20;
        final int N = 1000;
        final List<String> names = List.of("in-1", "in-2", "out-1");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        batchStorage.setWithHeader(false);
        final InputStream stream = batchStorage.getDataStream();
        final List<String> lines = BatchReader.readEntries(stream, batchSize);
        final ByteBuffer buffer = ByteBuffer.wrap(BatchReader.linesToBytes(lines));

        final List<String> inputNames = List.of("in-1", "in-2");
        final List<String> outputNames = List.of("out-1");
        final Metadata metadata = createMetadata(inputNames, outputNames);

        final CSVParser parser = new CSVParser();
        final Dataframe dataframe = parser.toDataframe(buffer, metadata);

        assertEquals(batchSize, dataframe.getRowDimension());
        assertEquals(inputNames, dataframe.getInputDataframe().getColumnNames());
        assertEquals(outputNames, dataframe.getOutputDataframe().getColumnNames());
        assertEquals(3, dataframe.getColumnDimension());
        final List<Integer> expected = IntStream.range(N - batchSize, N).boxed().collect(Collectors.toList());
        final List<Integer> actual = dataframe.getColumn(0).stream().map(v -> (int) Math.floor(v.asNumber())).collect(Collectors.toList());
        assertEquals(expected, actual);

    }

    @Test
    void testConvertToDataframeSmallerThanBatch() throws IOException {
        final int batchSize = 100;
        final int N = 90;
        final List<String> names = List.of("xa-1", "xa-2", "yb-3");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        batchStorage.setWithHeader(false);
        final InputStream stream = batchStorage.getDataStream();
        final List<String> lines = BatchReader.readEntries(stream, batchSize);
        final ByteBuffer buffer = ByteBuffer.wrap(BatchReader.linesToBytes(lines));

        final List<String> inputNames = List.of("xa-1", "xa-2");
        final List<String> outputNames = List.of("yb-3");
        final Metadata metadata = createMetadata(inputNames, outputNames);

        final CSVParser parser = new CSVParser();
        final Dataframe dataframe = parser.toDataframe(buffer, metadata);

        assertEquals(N, dataframe.getRowDimension());
        assertEquals(inputNames, dataframe.getInputNames());
        assertEquals(outputNames, dataframe.getOutputNames());
        assertEquals(3, dataframe.getColumnDimension());
        final List<Integer> expected = IntStream.range(0, N).boxed().collect(Collectors.toList());
        final List<Integer> actual = dataframe.getColumn(0).stream().map(v -> (int) Math.floor(v.asNumber())).collect(Collectors.toList());
        assertEquals(expected, actual);
    }

    @Test
    void testConvertToDataframeNoHeader() {
        final int batchSize = 100;
        final int N = 90;
        final List<String> names = List.of("xa-1", "xa-2", "xa-3", "ya-1");
        final BatchingStorage batchStorage = new BatchingStorage();
        batchStorage.setNames(names);
        batchStorage.setObservations(N);
        batchStorage.setWithHeader(false);
        final InputStream stream;
        try {
            stream = batchStorage.getDataStream();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        final List<String> lines;
        try {
            lines = BatchReader.readEntries(stream, batchSize);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final ByteBuffer inputBuffer = ByteBuffer.wrap(BatchReader.linesToBytes(lines));

        final List<String> inputNames = List.of("xa-1", "xa-2", "xa-3");
        final List<String> outputNames = List.of("ya-1");
        final Metadata metadata = createMetadata(inputNames, outputNames);

        final CSVParser parser = new CSVParser();

        final Dataframe dataframe = parser.toDataframe(inputBuffer, metadata);

        assertEquals(N, dataframe.getRowDimension());
        assertEquals(4, dataframe.getColumnDimension());
        assertEquals(inputNames, dataframe.getInputNames());
        assertEquals(outputNames, dataframe.getOutputNames());
    }
}
