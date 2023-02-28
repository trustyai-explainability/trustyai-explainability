package org.kie.trustyai.service.mocks;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.storage.Storage;

import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
public class MockMemoryStorage extends Storage {

    private final String dataFilename;
    private Map<String, String> data = new HashMap<>();

    public MockMemoryStorage() {
        this.dataFilename = "data.csv";
    }

    @Override
    public ByteBuffer getData() throws StorageReadException {
        if (data.containsKey(this.dataFilename)) {
            return ByteBuffer.wrap(data.get(this.dataFilename).getBytes());
        } else {
            throw new StorageReadException("Data file not found");
        }

    }

    @Override
    public boolean dataExists() throws StorageReadException {
        return data.containsKey(this.dataFilename);
    }

    @Override
    public void save(ByteBuffer data, String location) throws StorageWriteException {
        final String stringData = new String(data.array(), StandardCharsets.UTF_8);
        this.data.put(location, stringData);
    }

    @Override
    public void append(ByteBuffer data, String location) throws StorageWriteException {
        if (this.data.containsKey(location)) {
            final String existing = this.data.get(location);
            this.data.put(location, existing + new String(data.array(), StandardCharsets.UTF_8));
        } else {
            throw new StorageWriteException("Destination does not exist: " + location);
        }

    }

    @Override
    public void appendData(ByteBuffer data) throws StorageWriteException {
        append(data, this.dataFilename);
    }

    @Override
    public ByteBuffer read(String location) throws StorageReadException {
        if (data.containsKey(location)) {
            return ByteBuffer.wrap(data.get(location).getBytes());
        } else {
            throw new StorageReadException("File not found: " + location);
        }

    }

    @Override
    public void saveData(ByteBuffer data) throws StorageWriteException {
        save(data, this.dataFilename);
    }

    @Override
    public boolean fileExists(String location) throws StorageReadException {
        return data.containsKey(location);
    }

    public void emptyStorage() {
        this.data = new HashMap<>();
    }

    public void reset() {
        this.data = new HashMap<>();
    }
}
