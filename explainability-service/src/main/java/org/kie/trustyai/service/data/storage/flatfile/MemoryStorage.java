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
    public ByteBuffer readDataframe(final String modelId) throws StorageReadException {
        LOG.debug("Cache miss. Reading data for " + modelId);
        return readDataframe(modelId, this.batchSize);
    }

    @Override
    public ByteBuffer readDataframe(String modelId, int batchSize) throws StorageReadException {
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
    public ByteBuffer readDataframe(String modelId, int startPos, int endPos) throws StorageReadException {
        throw new NotImplementedException("Sliced file reads not supported by MemoryStorage");
    }

    @Override
    public boolean dataExists(String modelId) throws StorageReadException {
        return data.containsKey(getDataFilename(modelId));
    }

    @Override
    public void saveDataframe(ByteBuffer data, String location) throws StorageWriteException {
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
    public void appendMetaOrInternalData(ByteBuffer data, String modelId) throws StorageWriteException {
        append(data, getDataFilename(modelId));
    }

    @Override
    public ByteBuffer readMetaOrInternalData(String location) throws StorageReadException {
        if (data.containsKey(location)) {
            return ByteBuffer.wrap(data.get(location).getBytes());
        } else {
            throw new StorageReadException("File not found: " + location);
        }
    }

    /**
     * Read {@link ByteBuffer} from the memory storage, for a given filename and batch size.
     * 
     * @param location The filename to read
     * @param batchSize The batch size
     * @return A {@link ByteBuffer} containing the data
     * @throws StorageReadException If an error occurs while reading the data
     */
    @Override
    public ByteBuffer readMetaOrInternalData(String location, int batchSize) throws StorageReadException {
        if (data.containsKey(location)) {
            final String content = data.get(location);
            final String[] lines = content.split("\n");

            final int start = Math.max(0, lines.length - batchSize);

            final StringBuilder lastLines = new StringBuilder();
            for (int i = start; i < lines.length; i++) {
                lastLines.append(lines[i]);
                if (i < lines.length - 1) {
                    lastLines.append("\n");
                }
            }
            return ByteBuffer.wrap(lastLines.toString().getBytes());
        } else {
            throw new StorageReadException("File not found: " + location);
        }
    }

    @Override
    public ByteBuffer readMetaOrInternalData(String location, int startPos, int endPos) throws StorageReadException {
        if (data.containsKey(location)) {
            final String content = data.get(location);
            final String[] lines = content.split("\n");

            final StringBuilder lastLines = new StringBuilder();
            int endPosAdj = Math.min(endPos, lines.length);

            for (int i = startPos; i < endPosAdj; i++) {
                lastLines.append(lines[i]);
                if (i < endPosAdj - 1) {
                    lastLines.append("\n");
                }
            }
            return ByteBuffer.wrap(lastLines.toString().getBytes());
        } else {
            throw new StorageReadException("File not found: " + location);
        }
    }

    @Override
    public void saveMetaOrInternalData(ByteBuffer data, String modelId) throws StorageWriteException {
        saveDataframe(data, getDataFilename(modelId));
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
        crc32.update(readDataframe(modelId));
        return crc32.getValue();
    }
}
