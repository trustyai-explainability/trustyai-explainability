package org.kie.trustyai.service.data.datasources;

import java.util.Set;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.storage.hibernate.HibernateStorage;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.payloads.service.NameMapping;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@LookupIfProperty(name = "service.data.format", stringValue = "HIBERNATE")
public class HibernateDataSource extends DataSource {
    @Inject
    Instance<HibernateStorage> storage;

    private HibernateStorage getStorage() {
        return storage.get();
    }

    // DATAFRAME READS =================================================================================================
    /**
     * Using the default batch size, get a dataframe consisting of the last $defaultBatchSize rows of data from
     * the corresponding model.
     *
     * @param modelId the model id
     * @return a dataframe with the last $defaultBatchSize rows of data from the corresponding model.
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframe(final String modelId) throws DataframeCreateException {
        HibernateStorage hst = getStorage();
        return hst.readDataframe(modelId);
    }

    /**
     * Get a dataframe consisting of the last $batchSize rows of data from the corresponding model.
     *
     * @param modelId the model id
     * @param batchSize the batchSize
     * @return a dataframe with the last $batchSize rows of data from the corresponding model.
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframe(final String modelId, int batchSize) throws DataframeCreateException {
        HibernateStorage hst = getStorage();
        return hst.readDataframe(modelId, batchSize);
    }

    /**
     * Get a dataframe consisting of the rows of data between $startPos (inclusive) and $endPos (exclusive).
     *
     * @param modelId the model id
     * @param startPos the beginning index to return in the dataframe slice, inclusive
     * @param endPos the ending index to return in the dataframe slice, exclusive
     *
     * @return a dataframe with rows of data between $startPos (inclusive) and $endPos (exclusive).
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframe(final String modelId, int startPos, int endPos) throws DataframeCreateException {
        if (endPos <= startPos) {
            throw new IllegalArgumentException("DataSource.getDataframe endPos must be greater than startPos. Got startPos=" + startPos + ", endPos=" + endPos);
        }

        HibernateStorage hst = getStorage();
        return hst.readDataframe(modelId, startPos, endPos);
    }

    /**
     * Get a dataframe with the organic (non-synthetic) data and metadata for a given model
     *
     * @param modelId the model id
     * @param batchSize the batch size
     * @return a dataframe with the organic data and metadata for a given model
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getOrganicDataframe(final String modelId, int batchSize) throws DataframeCreateException {
        HibernateStorage hst = getStorage();
        try {
            return hst.readDataframeAndMetadataWithTags(modelId, batchSize, Set.of(Dataframe.InternalTags.UNLABELED.get())).getLeft();
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }
    }

    /**
     * Get a dataframe with the organic (non-synthetic) data and metadata for a given model.
     * No batch size is given, so the default batch size is used.
     *
     * @param modelId the model id
     * @return a dataframe with the organic data and metadata for a given model
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getOrganicDataframe(final String modelId) throws DataframeCreateException {
        try {
            HibernateStorage hst = getStorage();
            return hst.readDataframeAndMetadataWithTags(modelId, Set.of(Dataframe.InternalTags.UNLABELED.get())).getLeft();
        } catch (StorageReadException e) {
            throw new DataframeCreateException(e.getMessage());
        }
    }

    // DATAFRAME WRITES ================================================================================================
    /**
     * Interface with the storage backend to execute a dataframe save
     *
     * @param dataframe the dataframe to save
     * @param modelId the model id
     * @param overwrite if true, overwrite any existing stored data for this dataframe with this one. Otherwise, append.
     * @throws InvalidSchemaException if the passed dataframe does not match the schema of existing data for the modelId.
     */
    protected void saveDataframeIntoStorage(final Dataframe dataframe, final String modelId, boolean overwrite) throws InvalidSchemaException {
        HibernateStorage hst = getStorage();
        if (!hst.dataExists(modelId)) {
            hst.saveDataframe(dataframe, modelId);
        } else if (overwrite) {
            hst.overwriteDataframe(dataframe, modelId);
        } else {
            hst.append(dataframe, modelId);
        }
    }

    // METADATA READS ==================================================================================================
    /**
     * Get metadata for this modelId, with optional loading of column enumerations
     *
     * @param modelId the model id
     * @param loadColumnValues if true, add column enumerations to the metadata. This adds an additional storage read,
     *        so use this only when necessary.
     * @throws StorageReadException if the metadata cannot be read
     */
    public StorageMetadata getMetadata(String modelId, boolean loadColumnValues) throws StorageReadException {
        HibernateStorage hibernateStorage = getStorage();
        StorageMetadata sm = hibernateStorage.readMetaOrInternalData(modelId);

        // only grab column enumerations from DB if explicitly requested, to save time
        if (loadColumnValues) {
            hibernateStorage.loadColumnValues(modelId, sm);
        }
        return sm;
    }

    /**
     * Check whether metadata exists for this modelId
     *
     * @param modelId the modelId to check for
     * @return true if metadata exists, false otherwise
     */
    public boolean hasMetadata(String modelId) {
        return getStorage().dataExists(modelId);
    }

    // METADATA WRITES =================================================================================================
    /**
     * Save metadata for this modelId
     *
     * @param storageMetadata the metadata to save
     * @param modelId the modelId to save this metadata under.
     * @throws StorageWriteException if the metadata cannot be saved.
     */
    public void saveMetadata(StorageMetadata storageMetadata, String modelId) throws StorageWriteException {
        getStorage().saveMetaOrInternalData(storageMetadata, modelId);
    }

    // DATAFRAME QUERIES ===============================================================================================
    /**
     * Get the number of observations for the corresponding model. The HibernateDataSource operation reads it directly
     * from the DB, to avoid loading the entire StorageMetadata.
     *
     * @param modelId the modelId to get the observation count for.
     * @return the number of observations
     */
    public long getNumObservations(String modelId) {
        return getStorage().rowCount(modelId);
    }

    /**
     * Check to see if a particular model has recorded inferences
     *
     * @param modelId the modelId to check
     * @return true if the model has received inference data
     */
    public boolean hasRecordedInferences(String modelId) {
        return getStorage().hasRecordedInferences(modelId);
    }

    // TAG OPERATIONS ==================================================================================================
    /**
     * Tag rows of a dataframe according to the tag mapping.
     *
     * @param dataTagging the dataTagging to apply. This contains both the modelId and the corresponding tag labels.
     */
    public void tagDataframeRows(DataTagging dataTagging) {
        getStorage().setTags(dataTagging);
    }

    // name aliasing handler
    public void applyNameMapping(NameMapping nameMapping) {
        getStorage().applyNameMapping(nameMapping);
    }

}
