package org.kie.trustyai.service.utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ResourceReader {
    public static String readFile(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null)
                return null;
            try (InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(isr)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }

    public static File resourceAsFile(String filename) throws IOException {
        final InputStream resourceStream = ResourceReader.class.getClassLoader().getResourceAsStream(filename);
        if (resourceStream == null) {
            throw new IllegalArgumentException("Resource not found: " + filename);
        }

        final Path tempFile = Files.createTempFile("temp-", filename.replaceAll("/", "_"));

        Files.copy(resourceStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        resourceStream.close();

        return tempFile.toFile();
    }
}
