package org.kie.trustyai.service.data.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.readers.PVCConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;

import io.quarkus.arc.lookup.LookupIfProperty;

@LookupIfProperty(name = "service.storage.format", stringValue = "PVC")
@ApplicationScoped
public class PVCStorage extends Storage {

    private static final Logger LOG = Logger.getLogger(PVCStorage.class);

    private final String inputFilename;
    private final String outputFilename;

    public PVCStorage(PVCConfig config) {
        LOG.info("Starting PVC storage consumer");
        this.inputFilename = config.inputFilename();
        this.outputFilename = config.outputFilename();
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
}
