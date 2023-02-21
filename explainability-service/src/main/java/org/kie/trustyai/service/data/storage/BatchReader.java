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
        String header = "";
        try (Scanner sc = new Scanner(stream, StandardCharsets.UTF_8)) {
            boolean capturedHeader = false;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                if (capturedHeader) {
                    queue.add(line);
                } else {
                    header = line;
                    capturedHeader = true;
                }

            }
            if (sc.ioException() != null) {
                throw sc.ioException();
            }
        }

        final List<String> entries = new ArrayList<>();
        entries.add(header);
        entries.addAll(new ArrayList<>(queue));
        return entries;
    }

    public static InputStream getDataInputStream(String filename) throws FileNotFoundException {
        return new FileInputStream(filename);
    }

    public static byte[] linesToBytes(List<String> lines) {
        return String.join("\n", lines).getBytes();
    }
}
