package org.kie.trustyai.service.data.storage;

public interface StorageInterface {
    DataFormat getDataFormat();

    long getLastModified(String modelId);
}
