package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.mocks.MockPVCStorage;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(PVCTestProfile.class)
class PVCStorageTest {

    private final static String TEST_DATA = "Test data";
    private final static ByteBuffer TEST_BUFFER = ByteBuffer.wrap(TEST_DATA.getBytes());

    private final static String EXTRA_DATA = " and some more";
    private final static ByteBuffer EXTRA_BUFFER = ByteBuffer.wrap(EXTRA_DATA.getBytes());

    private final static String MODEL_ID = "example-model";
    private final static String FILENAME = "file.txt";
    @Inject
    Instance<MockPVCStorage> storage;

    @BeforeEach
    void emptyStorage() {
        storage.get().emptyStorage("/tmp/" + FILENAME);
        storage.get().emptyStorage("/tmp/" + MODEL_ID + "-data.csv");
    }

    @Test
    void readData() {
        TEST_BUFFER.rewind();
        Exception exception = assertThrows(StorageReadException.class, () -> storage.get().readData(MODEL_ID));
        assertEquals("/tmp/example-model-data.csv (No such file or directory)", exception.getMessage());

        storage.get().saveData(TEST_BUFFER, MODEL_ID);
        final String readData = new String(storage.get().readData(MODEL_ID).array());
        assertEquals(TEST_DATA, readData);
    }

    @Test
    void save() {
        TEST_BUFFER.rewind();
        assertFalse(storage.get().fileExists(FILENAME));
        storage.get().save(TEST_BUFFER, FILENAME);
        assertTrue(storage.get().fileExists(FILENAME));
        final String readData = new String(storage.get().read(FILENAME).array());
        assertEquals(TEST_DATA, readData);
    }

    @Test
    void append() {
        assertFalse(storage.get().fileExists(FILENAME));
        Exception exception = assertThrows(StorageWriteException.class, () -> storage.get().append(TEST_BUFFER, FILENAME));
        assertEquals("Cannot append to non-existing file " + FILENAME, exception.getMessage());
        TEST_BUFFER.rewind();
        storage.get().save(TEST_BUFFER, FILENAME);
        assertTrue(storage.get().fileExists(FILENAME));
        EXTRA_BUFFER.rewind();
        storage.get().append(EXTRA_BUFFER, FILENAME);

        final String readData = new String(storage.get().read(FILENAME).array());
        assertEquals(TEST_DATA + EXTRA_DATA, readData);
    }

    @Test
    void read() {
        TEST_BUFFER.rewind();
        assertFalse(storage.get().fileExists(FILENAME));
        Exception exception = assertThrows(StorageWriteException.class, () -> storage.get().read(FILENAME));
        assertEquals("/tmp/" + FILENAME + " (No such file or directory)", exception.getMessage());
        storage.get().save(TEST_BUFFER, FILENAME);
        final String readData = new String(storage.get().read(FILENAME).array(), StandardCharsets.UTF_8);
        assertEquals(TEST_DATA, readData);
    }

    @Test
    void saveData() {
        EXTRA_BUFFER.rewind();
        assertFalse(storage.get().dataExists(MODEL_ID));
        storage.get().saveData(EXTRA_BUFFER, MODEL_ID);
        assertTrue(storage.get().dataExists(MODEL_ID));
        final String readData = new String(storage.get().readData(MODEL_ID).array());
        assertEquals(EXTRA_DATA, readData);
    }

    @Test
    void appendData() {
        TEST_BUFFER.rewind();
        assertFalse(storage.get().dataExists(MODEL_ID));
        final MockPVCStorage pvc = storage.get();
        Exception exception = assertThrows(StorageWriteException.class, () -> {
            pvc.appendData(TEST_BUFFER, MODEL_ID);
        });
        assertEquals("Cannot append to non-existing file " + MODEL_ID + "-data.csv", exception.getMessage());
        TEST_BUFFER.rewind();
        storage.get().saveData(TEST_BUFFER, MODEL_ID);
        assertTrue(storage.get().dataExists(MODEL_ID));
        EXTRA_BUFFER.rewind();
        storage.get().appendData(EXTRA_BUFFER, MODEL_ID);

        final String readData = new String(storage.get().readData(MODEL_ID).array());
        assertEquals(TEST_DATA + EXTRA_DATA, readData);

    }

    @Test
    void fileExists() {
        TEST_BUFFER.rewind();
        assertFalse(storage.get().fileExists(FILENAME));
        storage.get().save(TEST_BUFFER, FILENAME);
        assertTrue(storage.get().fileExists(FILENAME));
    }

    @Test
    void dataExists() {
        assertFalse(storage.get().dataExists(MODEL_ID));
        storage.get().saveData(TEST_BUFFER, MODEL_ID);
        assertTrue(storage.get().dataExists(MODEL_ID));
    }

    @Test
    void getDataFilename() {
        final String filename = storage.get().getDataFilename(MODEL_ID);
        assertEquals("example-model-data.csv", filename);
    }

    @Test
    void buildDataPath() {
        final Path path = storage.get().buildDataPath(MODEL_ID);
        assertEquals(Path.of("/tmp", MODEL_ID + "-data.csv"), path);
    }

    @Test
    void concurrentAppend() throws InterruptedException {
        final int threads = 20;
        final int N = 1000;
        final String data = "123456789";
        // create file
        storage.get().save(ByteBuffer.wrap((data + "\n").getBytes()), FILENAME);
        assertTrue(storage.get().fileExists(FILENAME));
        ExecutorService service = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            service.execute(() -> {
                for (int j = 0; j < N; j++) {
                    storage.get().append(ByteBuffer.wrap((data + "\n").getBytes()), FILENAME);
                }
                latch.countDown();
            });
        }
        latch.await();
        final String result = new String(storage.get().read(FILENAME).array());
        final String[] lines = result.split("\n");

        assertTrue(Arrays.stream(lines).allMatch(line -> line.equals(data)));
        assertEquals(N * threads + 1, lines.length);

    }

}