package org.kie.trustyai.service.data.storage.flatfile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;
import org.kie.trustyai.service.payloads.service.InferenceId;

import static org.kie.trustyai.service.data.datasources.DataSource.INTERNAL_DATA_FILENAME;

@LookupIfProperty(name = "service.storage-format", stringValue = "MEMORY")
@ApplicationScoped
public class MemoryStorage extends FlatFileStorage {

    private static final Logger LOG = Logger.getLogger(MemoryStorage.class);

    protected final Map<String, String> data = new ConcurrentHashMap<>();
    private final String dataFilename;
    private final int batchSize;

    public MemoryStorage(ServiceConfig serviceConfig, StorageConfig config) {
        super();
        LOG.info("Starting memory storage consumer: ");

        this.dataFilename = config.dataFilename().orElseThrow(() -> new IllegalArgumentException("Memory storage must provide a configured data filename in StorageConfig"));
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

    public Pair<ByteBuffer, ByteBuffer> readDataframeAndMetadataWithTags(String modelId, Set<String> tags) throws StorageReadException {
        return readDataframeAndMetadataTagFiltered(modelId, this.batchSize, tags, false);
    }

    public Pair<ByteBuffer, ByteBuffer> readDataframeAndMetadataWithTags(String modelId, int batchSize, Set<String> tags) throws StorageReadException {
        return readDataframeAndMetadataTagFiltered(modelId, batchSize, tags, false);
    }

    public Pair<ByteBuffer, ByteBuffer> readDataframeAndMetadataWithoutTags(String modelId, Set<String> tags) throws StorageReadException {
        return readDataframeAndMetadataTagFiltered(modelId, this.batchSize, tags, true);
    }

    public ByteBuffer readInferenceIds(String modelId, boolean onlyOrganic) throws StorageReadException {

        final String metadataKey = getInternalDataFilename(modelId);
        if (data.containsKey(metadataKey)) {
            String metadataContent = data.get(metadataKey);
            StringBuilder lines = new StringBuilder();
            try (CSVParser parser = CSVParser.parse(metadataContent, CSVFormat.DEFAULT.withTrim())) {
                for (CSVRecord record : parser) {
                    String metadataLine = record.get(0); // Assuming the tag is in the first column
                    boolean containsTags;
                    if (onlyOrganic) {
                        containsTags = !metadataLine.contains(Dataframe.InternalTags.SYNTHETIC.get());
                    } else {
                        containsTags = true;
                    }
                    if (containsTags) {
                        lines.append(record.get(1)).append(",").append(record.get(2)).append("\n");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (lines.length() > 0 && lines.charAt(lines.length() - 1) == '\n') {
                lines.deleteCharAt(lines.length() - 1);
            }
            return ByteBuffer.wrap(lines.toString().getBytes());

        } else {
            throw new StorageReadException("Data or Metadata file not found for modelId: " + modelId);
        }
    }

    @Override
    public ByteBuffer readAllInferenceIds(String modelId) throws StorageReadException {
        return readInferenceIds(modelId, false);
    }

    @Override
    public ByteBuffer readAllOrganicInferenceIds(String modelId) throws StorageReadException {
        return readInferenceIds(modelId, true);
    }

    public Pair<ByteBuffer, ByteBuffer> readDataframeAndMetadataWithoutTags(String modelId, int batchSize, Set<String> tags) throws StorageReadException {
        return readDataframeAndMetadataTagFiltered(modelId, batchSize, tags, true);
    }

    private Pair<ByteBuffer, ByteBuffer> readDataframeAndMetadataTagFiltered(String modelId, int batchSize, Set<String> tags, boolean invertTagFilter) throws StorageReadException {
        final List<String> dataLines = new ArrayList<>();
        final List<String> metadataLines = new ArrayList<>();

        final String dataKey = getDataFilename(modelId);
        final String metadataKey = getInternalDataFilename(modelId);

        if (data.containsKey(dataKey) && data.containsKey(metadataKey)) {
            String dataContent = data.get(dataKey);
            String metadataContent = data.get(metadataKey);
            String[] dataContentLines = dataContent.split("\n");

            try (CSVParser parser = CSVParser.parse(metadataContent, CSVFormat.DEFAULT.withTrim())) {
                int index = 0;
                for (CSVRecord record : parser) {
                    if (index >= dataContentLines.length) {
                        // Ensuring we do not go out of bounds if metadata lines are more than data lines
                        break;
                    }
                    String metadataLine = record.get(0); // Assuming the tag is in the first column
                    boolean containsTags = tags.contains(metadataLine);
                    if (invertTagFilter) {
                        containsTags = !containsTags;
                    }
                    if (containsTags) {
                        metadataLines.add(String.join(",", record));
                        dataLines.add(dataContentLines[index]);
                    }
                    index++;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Apply batch size limit
            if (dataLines.size() > batchSize) {
                final String dataLinesString = String.join("\n", dataLines.subList(dataLines.size() - batchSize, dataLines.size()));
                final String metadataLinesString = String.join("\n", metadataLines.subList(metadataLines.size() - batchSize, metadataLines.size()));
                return Pair.of(ByteBuffer.wrap(dataLinesString.getBytes()), ByteBuffer.wrap(metadataLinesString.getBytes()));
            } else {
                final String dataLinesString = String.join("\n", dataLines);
                final String metadataLinesString = String.join("\n", metadataLines);

                return Pair.of(ByteBuffer.wrap(dataLinesString.getBytes()), ByteBuffer.wrap(metadataLinesString.getBytes()));
            }
        } else {
            throw new StorageReadException("Data or Metadata file not found for modelId: " + modelId);
        }
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
     * <<<<<<< HEAD:explainability-service/src/main/java/org/kie/trustyai/service/data/storage/flatfile/MemoryStorage.java
     * 
     * @param location The filename to read
     *        =======
     *
     * @param location The filename to read
     *        <<<<<<< HEAD:explainability-service/src/main/java/org/kie/trustyai/service/data/storage/flatfile/MemoryStorage.java
     *        >>>>>>> 971331215cad9fd6a58c6be30edccb949c459c65:explainability-service/src/main/java/org/kie/trustyai/service/data/storage/MemoryStorage.java
     *        =======
     *        >>>>>>> 735c6e77a450570e5590dbd03c5f5e632de641d0:explainability-service/src/main/java/org/kie/trustyai/service/data/storage/MemoryStorage.java
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
    public String getInternalDataFilename(String modelId) {
        return modelId + "-" + INTERNAL_DATA_FILENAME;
    }

    @Override
    public Path buildDataPath(String modelId) {
        return Path.of(getDataFilename(modelId));
    }

    @Override
    public Path buildInternalDataPath(String modelId) {
        return Path.of(getInternalDataFilename(modelId));
    }

    @Override
    public long getLastModified(final String modelId) {
        final Checksum crc32 = new CRC32();
        crc32.update(readDataframe(modelId));
        return crc32.getValue();
    }
}
