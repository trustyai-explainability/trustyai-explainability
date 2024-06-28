package org.kie.trustyai.service.data.datasources;

import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;

// ERROR MESSAGES ==================================================================================================
// This class contains unified error messages for various datasource operations
class DataSourceErrors {
    static class DataframeLoad {
        static DataframeCreateException getBadSliceSortingError(String modelId, int startPos, int endPos) {
            return new DataframeCreateException(String.format("Error reading dataframe for model=%s: " +
                    "DataSource.getDataframe() endPos must be greater than startPos. Got startPos=%d, endPos=%d",
                    modelId, startPos, endPos));
        }

        static DataframeCreateException getReadError(String modelId, String errorMsg, String operation) {
            return new DataframeCreateException(String.format("Error %s for model=%s: %s", operation, modelId, errorMsg));
        }

        static DataframeCreateException getMetadataReadError(String modelId, String errorMsg) {
            return getReadError(modelId, errorMsg, "reading metadata");
        }

        static DataframeCreateException getDataframeReadError(String modelId, String errorMsg) {
            return getReadError(modelId, errorMsg, "reading dataframe");
        }

        static DataframeCreateException getInternalDataReadError(String modelId, String errorMsg) {
            return getReadError(modelId, errorMsg, "reading internal data");
        }

        static DataframeCreateException getDataframeCreateError(String modelId, String errorMsg) {
            return new DataframeCreateException(String.format("Error creating dataframe for model=%s: %s", modelId, errorMsg));
        }
    }

    static StorageReadException getMetadataReadError(String modelId, String errorMsg) {
        return getGenericReadError(modelId, errorMsg, "reading metadata");
    }

    static StorageReadException getDataframeAndMetadataReadError(String modelId, String errorMsg) {
        return getGenericReadError(modelId, errorMsg, "reading dataframe or metadata");
    }

    static StorageWriteException getMetadataSaveError(String modelId, String errorMsg) {
        return new StorageWriteException(String.format("Error saving metadata for model=%s: %s", modelId, errorMsg));
    }

    static StorageWriteException getDataframeSaveError(String modelId, String errorMsg) {
        return new StorageWriteException(String.format("Error saving dataframe for model=%s: %s", modelId, errorMsg));
    }

    static StorageWriteException getSchemaMismatchError(String modelId) {
        return new StorageWriteException(String.format("Payload schema does not match stored schema for model=%s", modelId));
    }

    static StorageReadException getGenericReadError(String modelId, String errorMsg, String operation) {
        return new StorageReadException(String.format("Error %s for model=%s: %s", operation, modelId, errorMsg));
    }
}
