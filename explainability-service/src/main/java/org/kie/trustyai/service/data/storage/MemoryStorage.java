package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.quarkus.arc.lookup.LookupIfProperty;

@LookupIfProperty(name = "service.storage.format", stringValue = "MEMORY")
@ApplicationScoped
public class MemoryStorage extends Storage {

    private static final Logger LOG = Logger.getLogger(MemoryStorage.class);

    protected final Map<String, String> data = new ConcurrentHashMap<>();
    private final String dataFilename;

    public MemoryStorage(StorageConfig config) {
        this.dataFilename = config.dataFilename();
    }

    @Override
    public ByteBuffer readData(final String modelId) throws StorageReadException {
        LOG.debug("Cache miss. Reading data for " + modelId);
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
        LOG.debug("Saving data to " + location);
        this.data.put(location, stringData);
    }

    @Override
    public void append(ByteBuffer data, String location) throws StorageWriteException {
        final String value = this.data.computeIfPresent(location, (key, existing) -> existing + new String(data.array(), StandardCharsets.UTF_8));
        LOG.debug("Appending data to " + location);
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
    public long getLastModified(final String modelId) {
        final Checksum crc32 = new CRC32();
        crc32.update(readData(modelId));
        return crc32.getValue();
    }
}
