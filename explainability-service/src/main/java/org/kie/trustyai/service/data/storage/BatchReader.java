package org.kie.trustyai.service.data.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;
import org.kie.trustyai.service.data.utils.CSVUtils;

public class BatchReader {

    private BatchReader() {
    }

    public static List<String> readEntries(InputStream stream, int batchSize) throws IOException {
        final CircularFifoQueue<String> queue = new CircularFifoQueue<>(batchSize);
        try (Scanner sc = new Scanner(stream, StandardCharsets.UTF_8)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                queue.add(line);
            }
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        }
        return new ArrayList<>(queue);
    }

    public static List<String> readEntries(InputStream stream, int startPos, int endPos) throws IOException {

        if (endPos <= startPos) {
            throw new IllegalArgumentException("BatchReader endPos must be greater than startPos. Got startPos=" + startPos + ", endPos=" + endPos);
        }

        final CircularFifoQueue<String> queue = new CircularFifoQueue<>(endPos - startPos);

        int readLines = 0;
        try (Scanner sc = new Scanner(stream, StandardCharsets.UTF_8)) {
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                queue.add(line);
                readLines += 1;
                if (readLines >= endPos) {
                    break;
                }
            }
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        }
        return new ArrayList<>(queue);
    }

    public static Pair<List<String>, List<String>> readEntriesWithTags(InputStream dataStream, InputStream metadataStream, int batchSize, Set<String> tags) throws IOException {
        final CircularFifoQueue<String> dataQueue = new CircularFifoQueue<>(batchSize);
        final CircularFifoQueue<String> metadataQueue = new CircularFifoQueue<>(batchSize);

        try (Scanner dataScanner = new Scanner(dataStream, StandardCharsets.UTF_8);
                BufferedReader metadataReader = new BufferedReader(new InputStreamReader(metadataStream, StandardCharsets.UTF_8))) {

            final CSVParser parser = new CSVParser(metadataReader, CSVFormat.DEFAULT.withSkipHeaderRecord());

            for (CSVRecord metadataRecord : parser) {
                if (dataScanner.hasNextLine()) {
                    final String dataLine = dataScanner.nextLine();
                    String metadataLine = CSVUtils.recordToString(metadataRecord);
                    String metadataTag = metadataRecord.get(0); // Tag is the first column

                    if (tags.contains(metadataTag)) {
                        dataQueue.add(dataLine);
                        metadataQueue.add(metadataLine);
                    }
                } else {
                    break; // No corresponding data line
                }
            }
            if (dataScanner.ioException() != null) {
                throw dataScanner.ioException();
            }
        }
        return Pair.of(new ArrayList<>(dataQueue), new ArrayList<>(metadataQueue));
    }

    /**
     * Returns an InputStream for the given filename
     *
     *
     * @param filename The filename to open
     * @return An {@link InputStream} for the given filename
     * @throws FileNotFoundException If the file does not exist
     */
    public static InputStream getFileInputStream(String filename) throws FileNotFoundException {
        return new FileInputStream(filename);
    }

    public static byte[] linesToBytes(List<String> lines) {
        return String.join("\n", lines).getBytes();
    }
}
