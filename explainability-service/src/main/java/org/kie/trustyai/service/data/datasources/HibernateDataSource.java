package org.kie.trustyai.service.data.datasources;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.storage.hibernate.HibernateStorage;
import org.kie.trustyai.service.data.storage.hibernate.migration.MigrationEvent;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.payloads.service.NameMapping;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@LookupIfProperty(name = "service.storage-format", stringValue = "DATABASE")
public class HibernateDataSource extends DataSource {
    @Inject
    Instance<HibernateStorage> storage;

    @Inject
    Event<MigrationEvent.MigrationTriggerEvent> migrationTriggerEvent;

    // mitigates https://github.com/quarkusio/quarkus/issues/2673
    private static volatile boolean migrationInProgress = false;
    private static volatile boolean migratedFromPreviousDB = false;

    @PostConstruct
    public void migrate() {
        // migrate any existing DFs in the DB
        List<String> previousModels = storage.get().getAllTrackedDataframes();
        if (!previousModels.isEmpty()) {
            List<String> msg = new ArrayList<>();
            msg.add("Inference data for the following models has been found in the database:");
            for (String model : previousModels) {
                addModelToKnown(model);
                msg.add(String.format("\t%s: %d rows", model, getNumObservations(model)));
            }
            msg.add("These models have been registered with TrustyAI.");
            migratedFromPreviousDB = true;
            LOG.info(String.join("\n", msg));
        }

        // PVC migration
        synchronized (this) {
            if (!migrationInProgress) {
                migrationInProgress = true;
                migrationTriggerEvent.fireAsync(MigrationEvent.getMigrationTriggerEvent());
            }
        }
    }

    void acknowledgeSingleDataframeMigration(@Observes MigrationEvent.MigrationSingleDataframeFinishEvent event) {
        addModelToKnown(event.getMigratedModel());
    }

    static void acknowledgeMigration(@Observes MigrationEvent.MigrationFinishEvent event) {
        migrationInProgress = false;
    }

    public boolean isMigrationInProgress() {
        return migrationInProgress;
    }

    public boolean isMigratedFromPreviousDB() {
        return migratedFromPreviousDB;
    }

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
        try {
            return hst.readDataframe(modelId);
        } catch (StorageReadException e) {
            throw DataSourceErrors.DataframeLoad.getDataframeReadError(modelId, e.getMessage());
        }
    }

    /**
     * Get a dataframe consisting of the last $batchSize rows of data from the corresponding model.
     *
     * @param modelId the model id
     * @param batchSize the batchSize
     * @return a dataframe with the last $batchSize rows of data from the corresponding model.
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframe(final String modelId, int batchSize) {
        HibernateStorage hst = getStorage();
        try {
            return hst.readDataframe(modelId, batchSize);
        } catch (StorageReadException e) {
            throw DataSourceErrors.DataframeLoad.getDataframeReadError(modelId, e.getMessage());
        } catch (DataframeCreateException e) {
            throw DataSourceErrors.DataframeLoad.getDataframeCreateError(modelId, e.getMessage());
        }
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
            throw DataSourceErrors.DataframeLoad.getBadSliceSortingError(modelId, startPos, endPos);
        }

        HibernateStorage hst = getStorage();
        try {
            return hst.readDataframe(modelId, startPos, endPos);
        } catch (StorageReadException e) {
            throw DataSourceErrors.DataframeLoad.getDataframeReadError(modelId, e.getMessage());
        } catch (DataframeCreateException e) {
            throw DataSourceErrors.DataframeLoad.getDataframeCreateError(modelId, e.getMessage());
        }
    }

    /**
     * Get a dataframe with matching tags data and metadata for a given model.
     *
     * @param modelId the model id
     * @param batchSize the batch size
     * @param tags the set of tags to include
     * @return a dataframe with matching tags
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframeFilteredByTags(final String modelId, int batchSize, Set<String> tags) throws DataframeCreateException {
        try {
            HibernateStorage hst = getStorage();
            return hst.readDataframeAndMetadataWithTags(modelId, batchSize, tags).getLeft();
        } catch (StorageReadException e) {
            throw DataSourceErrors.getDataframeAndMetadataReadError(modelId, e.getMessage());
        } catch (DataframeCreateException e) {
            throw DataSourceErrors.DataframeLoad.getDataframeCreateError(modelId, e.getMessage());
        }
    }

    /**
     * Get a dataframe with matching tags data and metadata for a given model.
     * No batch size is given, so the default batch size is used.
     *
     * @param modelId the model id
     * @param tags the set of tags to include
     * @return a dataframe with matching tags
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframeFilteredByTags(final String modelId, Set<String> tags) throws DataframeCreateException {
        try {
            HibernateStorage hst = getStorage();
            return hst.readDataframeAndMetadataWithTags(modelId, tags).getLeft();
        } catch (StorageReadException e) {
            throw DataSourceErrors.getDataframeAndMetadataReadError(modelId, e.getMessage());
        } catch (DataframeCreateException e) {
            throw DataSourceErrors.DataframeLoad.getDataframeCreateError(modelId, e.getMessage());
        }
    }

    /**
     * Get a dataframe with matching tags data and metadata for a given model.
     *
     * @param modelId the model id
     * @param batchSize the batch size
     * @param tags the set of tags to include
     * @return a dataframe with matching tags
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframeFilteredByNotTags(final String modelId, int batchSize, Set<String> tags) throws DataframeCreateException {
        try {
            HibernateStorage hst = getStorage();
            return hst.readDataframeAndMetadataWithoutTags(modelId, batchSize, tags).getLeft();
        } catch (StorageReadException e) {
            throw DataSourceErrors.getDataframeAndMetadataReadError(modelId, e.getMessage());
        }
    }

    /**
     * Get a dataframe with matching tags data and metadata for a given model.
     * No batch size is given, so the default batch size is used.
     *
     * @param modelId the model id
     * @param tags the set of tags to include
     * @return a dataframe with matching tags
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public Dataframe getDataframeFilteredByNotTags(final String modelId, Set<String> tags) throws DataframeCreateException {
        try {
            HibernateStorage hst = getStorage();
            return hst.readDataframeAndMetadataWithoutTags(modelId, tags).getLeft();
        } catch (StorageReadException e) {
            throw DataSourceErrors.getDataframeAndMetadataReadError(modelId, e.getMessage());
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
    protected void saveDataframeIntoStorage(final Dataframe dataframe, final String modelId, boolean overwrite) throws StorageWriteException {
        HibernateStorage hst = getStorage();
        try {
            if (!hst.dataExists(modelId)) {
                hst.saveDataframe(dataframe, modelId);
            } else if (overwrite) {
                hst.overwriteDataframe(dataframe, modelId);
            } else {
                hst.append(dataframe, modelId);
            }
        } catch (StorageWriteException e) {
            throw DataSourceErrors.getDataframeSaveError(modelId, e.getMessage());
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

        StorageMetadata sm;
        try {
            sm = hibernateStorage.readMetaOrInternalData(modelId);
        } catch (StorageReadException e) {

            throw DataSourceErrors.getMetadataReadError(modelId, e.getMessage());
        }

        // only grab column enumerations from DB if explicitly requested, to save time
        long startt = System.currentTimeMillis();
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
        try {
            getStorage().saveMetaOrInternalData(storageMetadata, modelId);
        } catch (StorageWriteException e) {
            throw DataSourceErrors.getMetadataSaveError(modelId, e.getMessage());
        }
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
        try {
            return getStorage().rowCount(modelId);
        } catch (Exception e) {
            throw DataSourceErrors.getGenericReadError(modelId, e.getMessage(), "retrieving row count");
        }
    }

    /**
     * Check to see if a particular model has recorded inferences
     *
     * @param modelId the modelId to check
     * @return true if the model has received inference data
     */
    public boolean hasRecordedInferences(String modelId) {
        try {
            return getStorage().hasRecordedInferences(modelId);
        } catch (Exception e) {
            throw DataSourceErrors.getGenericReadError(modelId, e.getMessage(), "checking for recorded inferences");
        }
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

    @Override
    public List<String> getTags(String modelId) {
        try {
            return getStorage().getTags(modelId);
        } catch (Exception e) {
            throw DataSourceErrors.getGenericReadError(modelId, e.getMessage(), "retrieving tags");
        }
    }

    // name aliasing handler
    public void applyNameMapping(NameMapping nameMapping) {
        getStorage().applyNameMapping(nameMapping);
    }

    /**
     * Clear a name mapping from a dataframe
     *
     * @param modelId the model for which to clear the name mappings
     */
    public void clearNameMapping(String modelId) {
        getStorage().clearNameMapping(modelId);
    }

}
