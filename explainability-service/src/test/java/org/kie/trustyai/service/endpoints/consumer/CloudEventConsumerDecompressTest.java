package org.kie.trustyai.service.endpoints.consumer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CloudEventConsumerDecompressTest {

    @Test
    void decompressValidGzip() throws Exception {
        byte[] original = "{\"model_name\":\"test\"}".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(original);
        }

        byte[] result = CloudEventConsumer.decompressIfGzip(baos.toByteArray());
        assertArrayEquals(original, result);
    }

    @Test
    void passthroughNonGzipData() {
        byte[] plainJson = "{\"model_name\":\"test\"}".getBytes(StandardCharsets.UTF_8);

        byte[] result = CloudEventConsumer.decompressIfGzip(plainJson);
        assertArrayEquals(plainJson, result);
    }

    @Test
    void passthroughEmptyData() {
        byte[] empty = new byte[0];

        byte[] result = CloudEventConsumer.decompressIfGzip(empty);
        assertEquals(0, result.length);
    }

    @Test
    void fallbackOnMalformedGzip() {
        // Starts with gzip magic bytes but is not valid gzip
        byte[] malformed = new byte[] { 0x1F, (byte) 0x8B, 0x00, 0x01, 0x02 };

        byte[] result = CloudEventConsumer.decompressIfGzip(malformed);
        assertArrayEquals(malformed, result);
    }
}
