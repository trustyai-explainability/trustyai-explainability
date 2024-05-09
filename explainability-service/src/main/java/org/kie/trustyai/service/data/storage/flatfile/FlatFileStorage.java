package org.kie.trustyai.service.data.storage.flatfile;

import java.nio.ByteBuffer;
import java.nio.file.Path;

import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.Storage;

public abstract class FlatFileStorage extends Storage<ByteBuffer, ByteBuffer> implements FlatFileStorageInterface {
    public DataFormat getDataFormat() {
        return DataFormat.CSV;
    }

    public abstract String getDataFilename(String modelId);

    /**
     * Get the internal data filename for a given model ID.
     *
     * @param modelId The model ID
     * @return The internal data filename
     */
    public abstract String getInternalDataFilename(String modelId);

    public abstract Path buildDataPath(String modelId);

    /**
     * Build the internal data path for a given model ID.
     *
     * @param modelId The model ID
     * @return The internal data path
     */
    public abstract Path buildInternalDataPath(String modelId);
}
