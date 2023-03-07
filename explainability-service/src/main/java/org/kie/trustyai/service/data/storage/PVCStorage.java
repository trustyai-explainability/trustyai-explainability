package org.kie.trustyai.service.data.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.quarkus.arc.lookup.LookupIfProperty;

@LookupIfProperty(name = "service.storage.format", stringValue = "PVC")
@ApplicationScoped
public class PVCStorage extends Storage {

    private static final Logger LOG = Logger.getLogger(PVCStorage.class);

    private final int batchSize;

    private final Path metadataPath;
    private final Path dataPath;
    private final String dataFilename;

    private final Path dataFolder;

    public PVCStorage(ServiceConfig serviceConfig, StorageConfig storageConfig) {
        LOG.info("Starting PVC storage consumer");
        if (serviceConfig.batchSize().isPresent()) {
            this.batchSize = serviceConfig.batchSize().getAsInt();
        } else {
            final String message = "Missing data batch size";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }

        this.metadataPath = Paths.get(storageConfig.dataFolder(), DataSource.METADATA_FILENAME);
        this.dataFilename = storageConfig.dataFilename();
        this.dataPath = Paths.get(storageConfig.dataFolder(), storageConfig.dataFilename());
        this.dataFolder = Path.of(storageConfig.dataFolder());

        if (metadataPath.equals(dataPath)) {
            final String message = "Data file and metadata file cannot have the same name (" + this.dataPath + ")";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }

        LOG.info("PVC data locations: data=*-" + this.dataPath + ", metadata=*-" + this.metadataPath);
    }

    @Override
    public ByteBuffer readData(String modelId) throws StorageReadException {
        LOG.debug("Cache miss. Reading data for " + modelId);
        try {
            return ByteBuffer.wrap(
                    BatchReader.linesToBytes(
                            BatchReader.readEntries(
                                    BatchReader.getDataInputStream(
                                            buildDataPath(modelId).toString()),
                                    this.batchSize)));
        } catch (IOException e) {
            LOG.error("Error reading input file for model " + modelId);
            throw new StorageReadException(e.getMessage());
        }
    }

    private boolean pathExists(Path path) {
        return (path.toFile().exists() && path.toFile().isDirectory());
    }

    private boolean createPath(Path path) {
        return path.toFile().mkdirs();
    }

    private synchronized void writeData(ByteBuffer byteBuffer, Path path, boolean append) throws StorageWriteException, StorageReadException {
        final File file = path.toFile();
        final boolean exists = pathExists(path.getParent());
        if (!exists) {
            createPath(path.getParent());
        }

        try (FileChannel channel = new FileOutputStream(file, append).getChannel()) {
            channel.write(byteBuffer);
        } catch (IOException e) {
            throw new StorageWriteException(e.getMessage());
        }
    }

    @Override
    public long getLastModified(final String modelId) {
        final Path filepath = Paths.get(this.dataFolder.toString(), getDataFilename(modelId));
        return filepath.toFile().lastModified();
    }

    @Override
    public void save(ByteBuffer byteBuffer, String filename) throws StorageWriteException, StorageReadException {
        final Path filepath = Paths.get(this.dataFolder.toString(), filename);
        writeData(byteBuffer, filepath, false);
    }

    @Override
    public void append(ByteBuffer data, String location) throws StorageWriteException {
        final Path filepath = Paths.get(this.dataFolder.toString(), location);
        if (!filepath.toFile().exists()) {
            final String message = "Cannot append to non-existing file " + location;
            LOG.error(message);
            throw new StorageWriteException(message);
        } else {
            writeData(data, filepath, true);
        }
    }

    @Override
    public ByteBuffer read(String filename) throws StorageReadException {
        final Path path = Paths.get(this.dataFolder.toString(), filename);
        final File file = path.toFile();

        try {
            final FileInputStream stream = new FileInputStream(path.toFile());
            byte[] arr = new byte[(int) file.length()];
            stream.read(arr);
            stream.close();
            return ByteBuffer.wrap(arr);
        } catch (IOException e) {
            throw new StorageWriteException(e.getMessage());
        }
    }

    @Override
    public void saveData(ByteBuffer data, String modelId) throws StorageWriteException {
        save(data, getDataFilename(modelId));
    }

    @Override
    public void appendData(ByteBuffer byteBuffer, String modelId) throws StorageWriteException, StorageReadException {
        append(byteBuffer, getDataFilename(modelId));
    }

    @Override
    public boolean fileExists(String location) {
        final File f = Paths.get(this.dataFolder.toString(), location).toFile();
        return (f.exists() && !f.isDirectory());
    }

    @Override
    public boolean dataExists(String modelId) throws StorageReadException {
        return fileExists(getDataFilename(modelId));
    }

    @Override
    public String getDataFilename(String modelId) {
        return modelId + "-" + this.dataFilename;
    }

    @Override
    public Path buildDataPath(String modelId) {
        return Path.of(this.dataFolder.toString(), getDataFilename(modelId));
    }
}
