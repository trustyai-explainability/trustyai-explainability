package org.kie.trustyai.service.data.storage;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.collections4.queue.CircularFifoQueue;

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

    public static InputStream getDataInputStream(String filename) throws FileNotFoundException {
        return new FileInputStream(filename);
    }

    public static byte[] linesToBytes(List<String> lines) {
        return String.join("\n", lines).getBytes();
    }
}
