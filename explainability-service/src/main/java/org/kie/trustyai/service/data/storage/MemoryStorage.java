package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.ApplicationScoped;

import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.quarkus.arc.lookup.LookupIfProperty;
import io.quarkus.logging.Log;

@LookupIfProperty(name = "service.storage.format", stringValue = "MEMORY")
@ApplicationScoped
public class MemoryStorage extends Storage {

    protected final Map<String, String> data = new ConcurrentHashMap<>();
    private final String dataFilename;

    public MemoryStorage(StorageConfig config) {
        this.dataFilename = config.dataFilename();
    }

    @Override
    public ByteBuffer readData(final String modelId) throws StorageReadException {
        final String key = getDataFilename(modelId);
        if (data.containsKey(key)) {
            return ByteBuffer.wrap(data.get(key).getBytes());
        } else {
            throw new StorageReadException("Data file '" + key + "' not found");
        }

    }

    @Override
    public boolean dataExists(String modelId) throws StorageReadException {
        return data.containsKey(getDataFilename(modelId));
    }

    @Override
    public void save(ByteBuffer data, String location) throws StorageWriteException {
        final String stringData = new String(data.array(), StandardCharsets.UTF_8);
        Log.info("Saving " + stringData + " to " + location);
        this.data.put(location, stringData);
    }

    @Override
    public void append(ByteBuffer data, String location) throws StorageWriteException {
        final String value = this.data.computeIfPresent(location, (key, existing) -> existing + new String(data.array(), StandardCharsets.UTF_8));
        if (value == null) {
            throw new StorageWriteException("Destination does not exist: " + location);
        }
    }

    @Override
    public void appendData(ByteBuffer data, String modelId) throws StorageWriteException {
        append(data, getDataFilename(modelId));
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
    public void saveData(ByteBuffer data, String modelId) throws StorageWriteException {
        save(data, getDataFilename(modelId));
    }

    @Override
    public boolean fileExists(String location) throws StorageReadException {
        return data.containsKey(location);
    }

    @Override
    public String getDataFilename(String modelId) {
        return modelId + "-" + this.dataFilename;
    }

    @Override
    public Path buildDataPath(String modelId) {
        return Path.of(getDataFilename(modelId));
    }

    @Override
    public long getLastModified() {
        return data.get(config.inputFilename().get()).hashCode();
    }
}
