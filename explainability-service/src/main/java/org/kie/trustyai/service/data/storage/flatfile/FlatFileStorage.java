package org.kie.trustyai.service.data.storage.flatfile;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import org.kie.trustyai.service.data.storage.Storage;

public abstract class FlatFileStorage extends Storage<ByteBuffer, ByteBuffer> implements FlatFileStorageInterface {

    public abstract String getDataFilename(String modelId);

    public abstract Path buildDataPath(String modelId);
}
