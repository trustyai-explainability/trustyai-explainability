package org.kie.trustyai.service.data.storage;

public abstract class Storage<DATAFRAME_TYPE, AUX_DATA_TYPE> implements StorageInterface<DATAFRAME_TYPE, AUX_DATA_TYPE> {
    public DataFormat getDataFormat() {
        return DataFormat.CSV;
    }
}
