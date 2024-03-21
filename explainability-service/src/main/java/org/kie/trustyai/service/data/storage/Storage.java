package org.kie.trustyai.service.data.storage;

import org.kie.trustyai.service.data.exceptions.StorageWriteException;

public abstract class Storage implements StorageInterface {
    public DataFormat getDataFormat() {
        return DataFormat.CSV;
    }

    public void save(Object data, String location) throws StorageWriteException {

    }
}
