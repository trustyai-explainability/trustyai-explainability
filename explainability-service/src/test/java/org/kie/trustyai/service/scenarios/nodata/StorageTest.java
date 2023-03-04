package org.kie.trustyai.service.scenarios.nodata;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockMemoryStorage;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(BaseTestProfile.class)
class StorageTest {

    private final static String MODEL_ID = "non-existing-model";
    @Inject
    Instance<MockMemoryStorage> storage;
    @Inject
    Instance<MockDatasource> datasource;

    @BeforeEach
    void emptyStorage() {
        datasource.get().empty();
    }

    @Test
    void getData() {
        Exception exception = assertThrows(StorageReadException.class, () -> {
            storage.get().getData(MODEL_ID);
        });
        assertEquals("Data file not found", exception.getMessage());

    }

    @Test
    void dataExists() {
        assertFalse(storage.get().dataExists(MODEL_ID));
        storage.get().saveData(ByteBuffer.wrap("data".getBytes()), MODEL_ID);
        assertTrue(storage.get().dataExists(MODEL_ID));
    }

    @Test
    void save() {
        final String filename = "foobar";
        // Check file does not yet exist
        assertFalse(storage.get().fileExists(filename));
        final ByteBuffer byteBuffer = ByteBuffer.wrap("data".getBytes());
        // Save file
        storage.get().save(byteBuffer, filename);
        // Check file exists
        assertTrue(storage.get().fileExists(filename));
    }

    @Test
    void append() {
        final String filename = "foobar";
        final ByteBuffer byteBuffer = ByteBuffer.wrap("data".getBytes());
        // Append to yet non-existing file
        Exception exception = assertThrows(StorageWriteException.class, () -> {
            storage.get().append(byteBuffer, filename);
        });
        assertEquals("Destination does not exist: " + filename, exception.getMessage());
        // Save file
        storage.get().save(byteBuffer, filename);
        // Append
        storage.get().append(ByteBuffer.wrap(" and more".getBytes()), filename);
        // Check file exists
        assertTrue(storage.get().fileExists(filename));

        final ByteBuffer result = storage.get().read(filename);
        assertEquals("data and more", new String(result.array(), StandardCharsets.UTF_8));
    }

    @Test
    void appendData() {
        final ByteBuffer byteBuffer = ByteBuffer.wrap("data".getBytes());
        // Append to yet non-existing data file
        Exception exception = assertThrows(StorageWriteException.class, () -> {
            storage.get().appendData(byteBuffer, MODEL_ID);
        });
        assertEquals("Destination does not exist: " + MODEL_ID + "-data.csv", exception.getMessage());
        // Save file
        storage.get().saveData(byteBuffer, MODEL_ID);
        // Append
        storage.get().appendData(ByteBuffer.wrap(" and more".getBytes()), MODEL_ID);
        // Check file exists
        assertTrue(storage.get().dataExists(MODEL_ID));

        final ByteBuffer result = storage.get().getData(MODEL_ID);
        assertEquals("data and more", new String(result.array(), StandardCharsets.UTF_8));
    }

    @Test
    void read() {
        final String filename = "foobar";
        // Read from yet non-existing file
        Exception exception = assertThrows(StorageReadException.class, () -> {
            storage.get().read(filename);
        });
        assertEquals("File not found: " + filename, exception.getMessage());
        storage.get().save(ByteBuffer.wrap("data".getBytes()), filename);
        final ByteBuffer result = storage.get().read(filename);
        assertEquals("data", new String(result.array(), StandardCharsets.UTF_8));
    }

    @Test
    void saveData() {
        // Check data does not yet exist
        assertFalse(storage.get().dataExists(MODEL_ID));
        final ByteBuffer byteBuffer = ByteBuffer.wrap("data".getBytes());
        // Save data
        storage.get().saveData(byteBuffer, MODEL_ID);
        // Check data exists
        assertTrue(storage.get().dataExists(MODEL_ID));
    }

    @Test
    void fileExists() {
        final String filename = "foobar";
        assertFalse(storage.get().fileExists(filename));
        storage.get().save(ByteBuffer.wrap("data".getBytes()), filename);
        assertTrue(storage.get().fileExists(filename));
    }

}
