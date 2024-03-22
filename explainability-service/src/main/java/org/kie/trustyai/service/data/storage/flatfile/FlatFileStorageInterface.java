package org.kie.trustyai.service.data.storage.flatfile;

import java.nio.ByteBuffer;

import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.storage.StorageInterface;

public interface FlatFileStorageInterface extends StorageInterface<ByteBuffer, ByteBuffer> {
    void appendMetaOrInternalData(ByteBuffer data, String modelId) throws StorageWriteException;

    ByteBuffer readMetaOrInternalData(String location, int startPos, int endPos) throws StorageReadException;

    boolean fileExists(String location) throws StorageReadException;
}
