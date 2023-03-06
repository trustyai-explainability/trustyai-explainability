package org.kie.trustyai.service.data.storage;

import java.nio.file.Path;

public abstract class Storage implements StorageInterface {

    public abstract String getDataFilename(String modelId);

    public abstract Path buildDataPath(String modelId);

}
