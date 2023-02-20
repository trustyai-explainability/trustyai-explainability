package org.kie.trustyai.service.data.storage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
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

    public PVCStorage(PVCConfig config) {
        LOG.info("Starting PVC storage consumer");
        if (config.inputFilename().isEmpty()) {
            throw new IllegalArgumentException("Missing PVC input filename");
        } else {
            this.inputFilename = config.inputFilename().get();
        }
        if (config.outputFilename().isEmpty()) {
            throw new IllegalArgumentException("Missing PVC output filename");
        } else {
            this.outputFilename = config.outputFilename().get();
        }

        LOG.info("PVC data location: input file="
                + inputFilename
                + ", output filename=" + outputFilename);
    }

    private byte[] readFile(String filename) throws IOException {
        final Path path = Paths.get(filename);
        return Files.readAllBytes(path);
    }

    @Override
    public ByteBuffer getInputData() throws StorageReadException {
        try {
            return ByteBuffer.wrap(readFile(this.inputFilename));
        } catch (IOException e) {
            LOG.error("Error reading input file");
            throw new StorageReadException(e.getMessage());
        }
    }

    @Override
    public ByteBuffer getOutputData() throws StorageReadException {
        try {
            return ByteBuffer.wrap(readFile(this.outputFilename));
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
