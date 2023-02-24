package org.kie.trustyai.service.data.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.readers.PVCConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.quarkus.arc.lookup.LookupIfProperty;

@LookupIfProperty(name = "service.storage.format", stringValue = "PVC")
@ApplicationScoped
public class PVCStorage extends Storage {

    private static final Logger LOG = Logger.getLogger(PVCStorage.class);

    private final String inputFilename;
    private final String outputFilename;
    private final int batchSize;

    public PVCStorage(PVCConfig config, ServiceConfig serviceConfig) {
        LOG.info("Starting PVC storage consumer");
        if (config.inputFilename().isPresent()) {
            this.inputFilename = config.inputFilename().get();
        } else {
            final String message = "Missing PVC input filename";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        if (config.outputFilename().isPresent()) {
            this.outputFilename = config.outputFilename().get();
        } else {
            final String message = "Missing PVC output filename";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
        if (serviceConfig.batchSize().isPresent()) {
            this.batchSize = serviceConfig.batchSize().getAsInt();
        } else {
            final String message = "Missing data batch size";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }

        LOG.info("PVC data location: input file="
                + inputFilename
                + ", output filename=" + outputFilename);
    }

    @Override
    public ByteBuffer getInputData() throws StorageReadException {
        try {
            return ByteBuffer.wrap(BatchReader.linesToBytes(BatchReader.readEntries(BatchReader.getDataInputStream(this.inputFilename), this.batchSize)));
        } catch (IOException e) {
            LOG.error("Error reading input file");
            throw new StorageReadException(e.getMessage());
        }
    }

    @Override
    public ByteBuffer getOutputData() throws StorageReadException {
        try {
            return ByteBuffer.wrap(BatchReader.linesToBytes(BatchReader.readEntries(BatchReader.getDataInputStream(this.outputFilename), this.batchSize)));
        } catch (IOException e) {
            LOG.error("Error reading output file");
            throw new StorageReadException(e.getMessage());
        }
    }

    private boolean pathExists(Path path) {
        return (path.toFile().exists() && path.toFile().isDirectory());
    }

    private boolean createPath(Path path) {
        return path.toFile().mkdirs();
    }

    private synchronized void writeData(ByteBuffer byteBuffer, String filename, boolean append) throws StorageWriteException, StorageReadException {
        final File file = new File(filename);
        final Path parent = file.toPath().getParent();
        final boolean exists = pathExists(parent);
        if (!exists) {
            createPath(parent);
        }

        try (FileChannel channel = new FileOutputStream(filename, append).getChannel()) {
            channel.write(byteBuffer);
        } catch (IOException e) {
            throw new StorageWriteException(e.getMessage());
        }
    }

    private void saveData(ByteBuffer byteBuffer, String filename) throws StorageWriteException, StorageReadException {
        writeData(byteBuffer, filename, false);
    }

    private void appendData(ByteBuffer byteBuffer, String filename) throws StorageWriteException, StorageReadException {
        writeData(byteBuffer, filename, true);
    }

    @Override
    public void saveInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        saveData(byteBuffer, inputFilename);
    }

    @Override
    public void saveOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        saveData(byteBuffer, outputFilename);
    }

    @Override
    public void appendInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        appendData(byteBuffer, inputFilename);
    }

    @Override
    public void appendOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        appendData(byteBuffer, outputFilename);
    }

    private boolean fileExists(String filename) {
        final File f = new File(filename);
        return (f.exists() && !f.isDirectory());
    }

    @Override
    public boolean inputExists() throws StorageReadException {
        return fileExists(inputFilename);
    }

    @Override
    public boolean outputExists() throws StorageReadException {
        return fileExists(outputFilename);
    }
}
