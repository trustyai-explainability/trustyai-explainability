package org.kie.trustyai.service.data.storage;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.kie.trustyai.service.config.readers.MemoryConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

import io.quarkus.arc.lookup.LookupIfProperty;

@LookupIfProperty(name = "service.storage.format", stringValue = "MEMORY")
@ApplicationScoped
public class MemoryStorage extends Storage {

    private final Map<String, String> data = new HashMap<>();
    private final MemoryConfig config;

    public MemoryStorage(MemoryConfig config) {
        this.config = config;
    }

    @Override
    public ByteBuffer getInputData() throws StorageReadException {
        return ByteBuffer.wrap(data.get(config.inputFilename().get()).getBytes());
    }

    @Override
    public ByteBuffer getOutputData() throws StorageReadException {
        return ByteBuffer.wrap(data.get(config.outputFilename().get()).getBytes());
    }

    @Override
    public void saveInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        data.put(config.inputFilename().get(), new String(byteBuffer.array(), StandardCharsets.UTF_8));
    }

    @Override
    public void saveOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        data.put(config.outputFilename().get(), new String(byteBuffer.array(), StandardCharsets.UTF_8));
    }

    @Override
    public void appendInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        data.put(config.inputFilename().get(), data.get(config.inputFilename().get()) + new String(byteBuffer.array(), StandardCharsets.UTF_8));
    }

    @Override
    public void appendOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {
        data.put(config.outputFilename().get(), data.get(config.outputFilename().get()) + new String(byteBuffer.array(), StandardCharsets.UTF_8));
    }

    @Override
    public boolean inputExists() throws StorageReadException {
        return data.containsKey(config.inputFilename().get());
    }

    @Override
    public boolean outputExists() throws StorageReadException {
        return data.containsKey(config.outputFilename().get());
    }

    @Override
    public long getLastModified() {
        return data.get(config.inputFilename().get()).hashCode();
    }
}
