package org.kie.trustyai.service.endpoints.metrics;

import java.nio.ByteBuffer;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.storage.Storage;

import io.quarkus.test.Mock;

@Mock
@Alternative
@ApplicationScoped
public class MockMetricsStorage extends Storage {
    @Override
    public ByteBuffer getInputData() throws StorageReadException {
        return null;
    }

    @Override
    public ByteBuffer getOutputData() throws StorageReadException {
        return null;
    }

    @Override
    public void saveInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {

    }

    @Override
    public void saveOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {

    }

    @Override
    public void appendInputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {

    }

    @Override
    public void appendOutputData(ByteBuffer byteBuffer) throws StorageWriteException, StorageReadException {

    }

    @Override
    public boolean inputExists() throws StorageReadException {
        return false;
    }

    @Override
    public boolean outputExists() throws StorageReadException {
        return false;
    }

    @Override
    public long getLastModified() {
        return new Random().nextLong();
    }
}
