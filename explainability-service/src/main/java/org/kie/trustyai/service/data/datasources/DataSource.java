package org.kie.trustyai.service.data.datasources;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.InvalidSchemaException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.parsers.DataParser;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.data.utils.MetadataUtils;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.Schema;

import io.vertx.core.impl.ConcurrentHashSet;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

public abstract class DataSource {
    public static final String METADATA_FILENAME = "metadata.json";
    public static final String GROUND_TRUTH_SUFFIX = "-ground-truths";
    public static final String INTERNAL_DATA_FILENAME = "internal_data.csv";
    protected static final Logger LOG = Logger.getLogger(DataSource.class);

    protected final Set<String> knownModels = new ConcurrentHashSet<>();

    @Inject
    Instance<Storage<?, ?>> storage;

    @Inject
    DataParser parser;
    @Inject
    ServiceConfig serviceConfig;

    public void setParser(DataParser parser) {
        this.parser = parser;
    }

    // MODEL TRACKING OPERATIONS =======================================================================================
    public Set<String> getKnownModels() {
        return knownModels;
    }

    public void addModelToKnown(String modelId) {
        this.knownModels.add(modelId);
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
    public abstract Dataframe getDataframe(final String modelId) throws DataframeCreateException;

    /**
     * Get a dataframe consisting of the last $batchSize rows of data from the corresponding model.
     *
     * @param modelId the model id
     * @param batchSize the batchSize
     * @return a dataframe with the last $batchSize rows of data from the corresponding model.
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public abstract Dataframe getDataframe(final String modelId, int batchSize) throws DataframeCreateException;

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
    public abstract Dataframe getDataframe(final String modelId, int startPos, int endPos) throws DataframeCreateException;

    /**
     * Get a dataframe with the organic (non-synthetic) data and metadata for a given model
     *
     * @param modelId the model id
     * @param batchSize the batch size
     * @return a dataframe with the organic data and metadata for a given model
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public abstract Dataframe getOrganicDataframe(final String modelId, int batchSize) throws DataframeCreateException;

    /**
     * Get a dataframe with the organic (non-synthetic) data and metadata for a given model.
     * No batch size is given, so the default batch size is used.
     *
     * @param modelId the model id
     * @return a dataframe with the organic data and metadata for a given model
     * @throws DataframeCreateException if the dataframe cannot be created
     */
    public abstract Dataframe getOrganicDataframe(final String modelId) throws DataframeCreateException;

    // DATAFRAME WRITES ================================================================================================
    public void saveDataframe(final Dataframe dataframe, final String modelId) throws InvalidSchemaException {
        saveDataframe(dataframe, modelId, false);
    }

    /**
     * Save a dataframe for the given modelId
     *
     * @param dataframe the dataframe to save
     * @param modelId the model id
     * @param overwrite if true, overwrite any existing stored data for this dataframe with this one. Otherwise, append.
     * @throws InvalidSchemaException if the passed dataframe does not match the schema of existing data for the modelId.
     */
    public void saveDataframe(final Dataframe dataframe, final String modelId, boolean overwrite) throws InvalidSchemaException {
        // Add to known models
        addModelToKnown(modelId);
        saveDataframeMetadataHanding(dataframe, modelId, overwrite);
        saveDataframeIntoStorage(dataframe, modelId, overwrite);
    }

    /**
     * Initialize or update metadata in preparation of dataframe save operation
     *
     * @param dataframe the dataframe to save
     * @param modelId the model id
     * @param overwrite if true, overwrite any existing stored data for this dataframe with this one. Otherwise, append.
     * @throws InvalidSchemaException if the passed dataframe does not match the schema of existing data for the modelId.
     */
    private void saveDataframeMetadataHanding(Dataframe dataframe, String modelId, boolean overwrite) {
        if (!hasMetadata(modelId) || overwrite) {
            // If metadata is not present, create it
            // alternatively, overwrite existing metadata if requested

            final StorageMetadata storageMetadata = new StorageMetadata();
            storageMetadata.setInputSchema(MetadataUtils.getInputSchema(dataframe));
            storageMetadata.setOutputSchema(MetadataUtils.getOutputSchema(dataframe));
            storageMetadata.setModelId(modelId);
            storageMetadata.setObservations(dataframe.getRowDimension());
            storageMetadata.setRecordedInferences(dataframe.getTags().contains(Dataframe.InternalTags.UNLABELED.get()));
            storageMetadata.setInputTensorName(dataframe.getInputTensorName());
            storageMetadata.setOutputTensorName(dataframe.getOutputTensorName());
            try {
                saveMetadata(storageMetadata, modelId);
            } catch (StorageWriteException e) {
                throw new DataframeCreateException(e.getMessage());
            }
        } else {
            // If metadata is present, just increment number of observations
            final StorageMetadata storageMetadata = getMetadata(modelId);

            // validate metadata
            Schema newInputSchema = MetadataUtils.getInputSchema(dataframe);
            Schema newOutputSchema = MetadataUtils.getOutputSchema(dataframe);

            if (storageMetadata.getInputSchema().equals(newInputSchema) && storageMetadata.getOutputSchema().equals(newOutputSchema)) {
                storageMetadata.incrementObservations(dataframe.getRowDimension());
                storageMetadata.setRecordedInferences(storageMetadata.isRecordedInferences() || dataframe.getTags().contains(Dataframe.InternalTags.UNLABELED.get()));

                // update value list
                storageMetadata.mergeInputSchema(newInputSchema);
                storageMetadata.mergeOutputSchema(newOutputSchema);

                try {
                    saveMetadata(storageMetadata, modelId);
                } catch (StorageWriteException e) {
                    throw new DataframeCreateException(e.getMessage());
                }
            } else {
                final String message = "Payload schema and stored schema are not the same";
                LOG.error(message);
                throw new InvalidSchemaException(message);
            }
        }
    }

    /**
     * Interface with the storage backend to execute a dataframe save
     *
     * @param dataframe the dataframe to save
     * @param modelId the model id
     * @param overwrite if true, overwrite any existing stored data for this dataframe with this one. Otherwise, append.
     * @throws InvalidSchemaException if the passed dataframe does not match the schema of existing data for the modelId.
     */
    protected abstract void saveDataframeIntoStorage(final Dataframe dataframe, final String modelId, boolean overwrite);

    // METADATA READS ==================================================================================================
    public StorageMetadata getMetadata(String modelId) throws StorageReadException {
        return getMetadata(modelId, false);
    }

    /**
     * Get metadata for this modelId, with optional loading of column enumerations
     *
     * @param modelId the model id
     * @param loadColumnValues if true, add column enumerations to the metadata. This adds an additional storage read,
     *        so use this only when necessary.
     * @throws StorageReadException if the metadata cannot be read
     */
    public abstract StorageMetadata getMetadata(String modelId, boolean loadColumnValues) throws StorageReadException;

    /**
     * Check whether metadata exists for this modelId
     *
     * @param modelId the modelId to check for
     * @return true if metadata exists, false otherwise
     */
    public abstract boolean hasMetadata(String modelId);

    // METADATA WRITES =================================================================================================
    /**
     * Save metadata for this modelId
     *
     * @param storageMetadata the metadata to save
     * @param modelId the modelId to save this metadata under.
     * @throws StorageWriteException if the metadata cannot be saved.
     */
    public abstract void saveMetadata(StorageMetadata storageMetadata, String modelId) throws StorageWriteException;

    /**
     * Increase the number of observations tracked in this modelId's metadata
     *
     * @param number the number of observations to add
     * @param modelId the modelId to increase the observation count
     */
    public void updateMetadataObservations(int number, String modelId) {
        final StorageMetadata storageMetadata = getMetadata(modelId);
        storageMetadata.incrementObservations(number);
        saveMetadata(storageMetadata, modelId);
    }

    // DATAFRAME QUERIES ===============================================================================================
    /**
     * Get the number of observations for the corresponding model.
     *
     * @param modelId the modelId to get the observation count for.
     * @return the number of observations
     */
    public abstract long getNumObservations(String modelId);

    /**
     * Check to see if a particular model has recorded inferences
     *
     * @param modelId the modelId to check
     * @return true if the model has received inference data
     */
    public abstract boolean hasRecordedInferences(String modelId);

    /**
     * @return the list of modelIds that are confirmed to have metadata in storage.
     */
    public List<String> getVerifiedModels() {
        return knownModels.stream().filter(this::hasMetadata).collect(Collectors.toList());
    }

    // GROUND TRUTH OPERATIONS =========================================================================================
    /**
     * @return get identifier for the ground-truth dataframe for this particular modelId
     */
    public static String getGroundTruthName(String modelId) {
        return modelId + GROUND_TRUTH_SUFFIX;
    }

    /**
     * @return true if there is metadata for the ground-truth dataframe for this particular modelId;
     */
    public boolean hasGroundTruths(String modelId) {
        return hasMetadata(getGroundTruthName(modelId));
    }

    /**
     * Get ground-truth dataframe for this particular modelId;
     *
     * @param modelId the modelId for which these groundTruths apply
     * @return the ground-truth dataframe
     */
    public Dataframe getGroundTruths(String modelId) {
        return getDataframe(DataSource.getGroundTruthName(modelId));
    }

    /**
     * Save a ground-truth dataframe for this particular modelId;
     *
     * @param groundTruthsDataframe the ground-truth dataframe to save
     * @param modelId the modelId for which these groundTruths apply
     */
    public void saveGroundTruths(Dataframe groundTruthsDataframe, String modelId) {
        saveDataframe(groundTruthsDataframe, getGroundTruthName(modelId));
    }

    // TAG OPERATIONS ==================================================================================================
    /**
     * Tag rows of a dataframe according to the tag mapping.
     *
     * @param dataTagging the dataTagging to apply. This contains both the modelId and the corresponding tag labels.
     */
    public abstract void tagDataframeRows(DataTagging dataTagging);

    // NAME MAPPING OPERATIONS =========================================================================================
    /**
     * Apply a name mapping to a dataframe
     *
     * @param nameMapping the nameMapping to apply. This contains both the modelId and the name mappings.
     */
    public abstract void applyNameMapping(NameMapping nameMapping);

}
