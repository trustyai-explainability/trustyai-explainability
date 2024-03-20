package org.kie.trustyai.service.data.storage.flatfile;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.lang3.NotImplementedException;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;

@LookupIfProperty(name = "service.storage.format", stringValue = "MEMORY")
@ApplicationScoped
public class MemoryStorage extends FlatFileStorage {

    private static final Logger LOG = Logger.getLogger(MemoryStorage.class);

    protected final Map<String, String> data = new ConcurrentHashMap<>();
    private final String dataFilename;
    private final int batchSize;

    public MemoryStorage(ServiceConfig serviceConfig, StorageConfig config) {
        super();
        this.dataFilename = config.dataFilename();
        this.batchSize = serviceConfig.batchSize().getAsInt();
    }

    @Override
    public ByteBuffer readData(final String modelId) throws StorageReadException {
        LOG.debug("Cache miss. Reading data for " + modelId);
        return readData(modelId, this.batchSize);
    }

    @Override
    public ByteBuffer readData(String modelId, int batchSize) throws StorageReadException {
        LOG.debug("Cache miss. Reading data for " + modelId);
        final String key = getDataFilename(modelId);
        if (data.containsKey(key)) {
            final String[] lines = data.get(key).split("\n");
            final int size = lines.length;
            if (size <= batchSize) {
                return ByteBuffer.wrap(data.get(key).getBytes());
            } else {
                final String lastLines = String.join("\n", Arrays.asList(lines).subList(size - batchSize, size));
                return ByteBuffer.wrap(lastLines.getBytes());
            }
        } else {
            throw new StorageReadException("Data file '" + key + "' not found");
        }
    }

    @Override
    public ByteBuffer readData(String modelId, int startPos, int endPos) throws StorageReadException {
        throw new NotImplementedException("Sliced file reads not supported by MemoryStorage");
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
    public ByteBuffer read(String location, int startPos, int endPos) throws StorageReadException {
        throw new NotImplementedException("Sliced file reads not supported by MemoryStorage");
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
