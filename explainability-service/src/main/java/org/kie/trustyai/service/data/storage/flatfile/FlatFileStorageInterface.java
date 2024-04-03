package org.kie.trustyai.service.data.storage.flatfile;

import java.nio.ByteBuffer;

import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.storage.StorageInterface;

public interface FlatFileStorageInterface extends StorageInterface<ByteBuffer, ByteBuffer> {
    void appendMetaOrInternalData(ByteBuffer data, String modelId) throws StorageWriteException;

    /**
     * Read {@link ByteBuffer} from the file system, for a given filename and batch size.
     *
     * @param modelId The location or filename to read
     * @param batchSize The batch size
     * @return A {@link ByteBuffer} containing the data
     * @throws StorageReadException If an error occurs while reading the data
     */
    ByteBuffer readMetaOrInternalData(String modelId, int batchSize) throws StorageReadException;

    ByteBuffer readMetaOrInternalData(String modelId, int startPos, int endPos) throws StorageReadException;

    boolean fileExists(String location) throws StorageReadException;

}
