package org.kie.trustyai.service.data.storage.hibernate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.model.dataframe.DataframeMetadata;
import org.kie.trustyai.explainability.model.dataframe.DataframeRow;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.storage.MigrationConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.payloads.service.DataTagging;
import org.kie.trustyai.service.payloads.service.InferenceId;
import org.kie.trustyai.service.payloads.service.NameMapping;
import org.kie.trustyai.service.payloads.service.Schema;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@LookupIfProperty(name = "service.storage.format", stringValue = "DATABASE")
@Singleton
public class HibernateStorage extends Storage<Dataframe, StorageMetadata> {
    @PersistenceContext
    EntityManager em;

    private static final Logger LOG = Logger.getLogger(HibernateStorage.class);

    private final int batchSize;
    private Optional<MigrationConfig> migrationConfig = Optional.empty();
    private boolean dataDirty = false;
    private final String NO_DATA_ERROR_MSG = "No inference data for that model found in database.";

    private Random rng = new Random(0);
    private long updateHash;

    public HibernateStorage(ServiceConfig serviceConfig, StorageConfig storageConfig) {
        LOG.info("Starting Hibernate storage consumer.");

        if (serviceConfig.batchSize().isPresent()) {
            this.batchSize = serviceConfig.batchSize().getAsInt();
        } else {
            final String message = "Missing data batch size";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }
    }

    @Override
    public DataFormat getDataFormat() {
        return DataFormat.HIBERNATE;
    }

    // MIGRATIONS ======================================================================================================

    // DATAFRAME READS =================================================================================================
    @Override
    @Transactional
    public Dataframe readDataframe(String modelId) throws StorageReadException {
        LOG.debug("Reading dataframe " + modelId + " from Hibernate");
        return readDataframe(modelId, batchSize);
    }

    @Transactional
    public Dataframe readAllData(String modelId) throws StorageReadException {
        if (dataExists(modelId)) {
            try {
                LOG.debug("Reading dataframe " + modelId + " from Hibernate");
                refreshIfDirty();
                List<DataframeRow> rows = em.createQuery("" +
                        "select dr from DataframeRow dr" +
                        " where dr.modelId = ?1 ", DataframeRow.class)
                        .setParameter(1, modelId)
                        .getResultList();
                DataframeMetadata dm = em.find(DataframeMetadata.class, modelId);
                return Dataframe.untranspose(rows, dm);
            } catch (Exception e) {
                LOG.error("Error reading all data for model=" + modelId);
                throw new StorageReadException(e.getMessage());
            }
        } else {
            throw new StorageReadException("Error reading all data for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    @Override
    @Transactional
    public Dataframe readDataframe(String modelId, int batchSize) throws StorageReadException {
        if (dataExists(modelId)) {
            try {
                LOG.debug("Reading dataframe " + modelId + " from Hibernate (batched)");
                refreshIfDirty();
                List<DataframeRow> rows = em.createQuery("" +
                        "select dr from DataframeRow dr" +
                        " where dr.modelId = ?1 " +
                        "order by dr.dbId DESC ", DataframeRow.class)
                        .setParameter(1, modelId)
                        .setMaxResults(batchSize).getResultList();

                Collections.reverse(rows);
                DataframeMetadata dm = em.find(DataframeMetadata.class, modelId);
                return Dataframe.untranspose(rows, dm);
            } catch (Exception e) {
                LOG.error("Error reading dataframe for model=" + modelId);
                throw new StorageReadException(e.getMessage());
            }
        } else {
            throw new StorageReadException("Error reading dataframe for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    @Override
    @Transactional
    public Dataframe readDataframe(String modelId, int startPos, int endPos) throws StorageReadException {
        if (endPos <= startPos) {
            throw new IllegalArgumentException("HibernateStorage.readData endPos must be greater than startPos. Got startPos=" + startPos + ", endPos=" + endPos);
        }

        if (dataExists(modelId)) {
            try {
                LOG.debug("Reading dataframe " + modelId + " from Hibernate (batched)");
                refreshIfDirty();
                List<DataframeRow> rows = getRowsBetween(modelId, startPos, endPos);
                DataframeMetadata dm = em.find(DataframeMetadata.class, modelId);
                return Dataframe.untranspose(rows, dm);
            } catch (Exception e) {
                LOG.error("Error reading dataframe for model=" + modelId);
                throw new StorageReadException(e.getMessage());
            }
        } else {
            throw new StorageReadException("Error reading dataframe for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    // DATAFRAME WRITES ================================================================================================
    @Override
    @Transactional
    public void saveDataframe(Dataframe dataframe, String modelId) throws StorageWriteException {
        try {
            LOG.debug("Writing dataframe=" + modelId + ", rows=" + dataframe.getRowDimension() + " to Hibernate");
            List<DataframeRow> transpose = dataframe.transpose(modelId);
            setDataDirty();
            for (DataframeRow dr : transpose) {
                em.persist(dr);
            }
            DataframeMetadata dm = dataframe.getMetadata();
            dm.setId(modelId);
            if (dataExists(modelId)) {
                em.merge(dm);
            } else {
                em.persist(dm);
            }
        } catch (Exception e) {
            LOG.error("Error saving dataframe for model=" + modelId);
            throw new StorageWriteException(e.getMessage());
        }
    }

    @Transactional
    public void overwriteDataframe(Dataframe dataframe, String modelId) throws StorageWriteException {
        if (dataExists(modelId)) {
            try {
                LOG.debug("Overwriting dataframe=" + modelId + ", rows=" + dataframe.getRowDimension() + " to Hibernate");
                setDataDirty();
                em.createQuery("" +
                        "DELETE from DataframeRow dr " +
                        "where dr.modelId = ?1")
                        .setParameter(1, modelId)
                        .executeUpdate();
                saveDataframe(dataframe, modelId);
            } catch (Exception e) {
                LOG.error("Error overwriting dataframe for model=" + modelId);
                throw new StorageWriteException(e.getMessage());
            }
        } else {
            throw new StorageWriteException("Error overwriting dataframe for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    @Override
    @Transactional
    public void append(Dataframe dataframe, String modelId) throws StorageWriteException {
        if (dataExists(modelId)) {
            try {
                LOG.debug("Appending " + dataframe.getRowDimension() + " new rows to dataframe=" + modelId + " within Hibernate");
                List<DataframeRow> transpose = dataframe.transpose(modelId);
                setDataDirty();
                for (DataframeRow dr : transpose) {
                    em.persist(dr);
                }
            } catch (Exception e) {
                LOG.error("Error appending to model=" + modelId);
                throw new StorageWriteException(e.getMessage());
            }
        } else {
            throw new StorageWriteException("Error appending to model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    // INDIVIDUAL ROW READS ========-===================================================================================
    private List<DataframeRow> getRowsBetween(String modelId, int startPos, int endPos) {
        refreshIfDirty();
        return em.createQuery("" +
                "select dr from DataframeRow dr" +
                " where dr.modelId = ?1 " +
                "order by dr.dbId ASC ", DataframeRow.class)
                .setParameter(1, modelId)
                .setFirstResult(startPos)
                .setMaxResults(endPos - startPos).getResultList();
    }

    private DataframeRow getRow(String modelId, int idx) {
        refreshIfDirty();
        return em.createQuery("" +
                "select dr from DataframeRow dr" +
                " where dr.modelId = ?1 " +
                "order by dr.dbId ASC ", DataframeRow.class)
                .setParameter(1, modelId)
                .setFirstResult(idx)
                .setMaxResults(1)
                .getSingleResult();
    }

    // SPECIFIC DATAFRAME QUERIES ======================================================================================
    // get the row count of a persisted dataframe
    @Transactional
    public int colCount(String modelId) {
        return em.createQuery("select size(dr.row) from DataframeRow dr where dr.modelId = ?1", int.class)
                .setParameter(1, modelId)
                .getSingleResult();
    }

    @Transactional
    public long rowCount(String modelId) {
        return em.createQuery("select count(dr) from DataframeRow dr WHERE dr.modelId = ?1", long.class)
                .setParameter(1, modelId)
                .getSingleResult();
    }

    public List<Value> getColumnValues(String modelId, String columnName) {
        if (dataExists(modelId)) {
            List<String> colNames = em.find(DataframeMetadata.class, modelId).getRawNames();
            if (colNames.contains(columnName)) {
                Integer targetColIdx = colNames.indexOf(columnName);
                refreshIfDirty();
                return em.createQuery(
                        "select dr.row from DataframeRow dr " +
                                "where dr.modelId = ?1 AND " +
                                "index(dr.row) = ?2 ",
                        Value.class)
                        .setParameter(1, modelId)
                        .setParameter(2, targetColIdx)
                        .getResultList();
            } else {
                throw new StorageReadException(String.format(
                        "Error reading column values for model=%s, column=%s. Column %s not within available model columns=%s",
                        modelId, columnName, columnName, colNames.toString()));
            }
        } else {
            throw new StorageReadException("Error reading column values for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    public List<Value> getUniqueColumnValues(String modelId, String columnName, int max) {
        if (dataExists(modelId)) {
            List<String> colNames = em.find(DataframeMetadata.class, modelId).getRawNames();
            if (colNames.contains(columnName)) {
                Integer targetColIdx = colNames.indexOf(columnName);
                refreshIfDirty();
                return em.createQuery(
                        "select DISTINCT dr.row from DataframeRow dr " +
                                "where dr.modelId = ?1 AND " +
                                "index(dr.row) = ?2 ",
                        Value.class)
                        .setParameter(1, modelId)
                        .setParameter(2, targetColIdx)
                        .setMaxResults(max)
                        .getResultList();
            } else {
                throw new StorageReadException(String.format(
                        "Error reading column values for model=%s, column=%s. Column %s not within available model columns=%s",
                        modelId, columnName, columnName, colNames.toString()));
            }
        } else {
            throw new StorageReadException("Error reading column values for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    //    private void setSchemaEnumeration(String modelId, Schema schema) {
    //        for (Map.Entry<String, SchemaItem> entry : schema.getItems().entrySet()) {
    //            Set<UnderlyingObject> columnValues = getUniqueColumnValues(modelId, entry.getKey(), ValueEnumerationUtils.MAX_VALUE_ENUMERATION + 1)
    //                    .stream()
    //                    .map(Value::getUnderlyingObjectContainer)
    //                    .collect(Collectors.toSet());
    //            entry.getValue().setValueEnumeration(ValueEnumerationUtils.fromEnforcedSubsetOfColumnValues(columnValues));
    //        }
    //    }

    //    @Transactional
    //    public void loadColumnValues(String modelId, StorageMetadata storageMetadata) {
    //        setSchemaEnumeration(modelId, storageMetadata.getInputSchema());
    //        setSchemaEnumeration(modelId, storageMetadata.getOutputSchema());
    //    }

    // METADATA READ + WRITES ==========================================================================================
    @Override
    public StorageMetadata readMetaOrInternalData(String modelId) throws StorageReadException {
        if (metadataExists(modelId)) {
            try {
                LOG.debug("Reading metadata for " + modelId + " from Hibernate");
                refreshIfDirty();
                StorageMetadata sm = em.find(StorageMetadata.class, modelId);
                return sm;
            } catch (Exception e) {
                LOG.error("Error reading metadata for model=" + modelId);
                throw new StorageReadException(e.getMessage());
            }
        } else {
            throw new StorageReadException("Error reading metadata for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    @Override
    @Transactional
    public void saveMetaOrInternalData(StorageMetadata storageMetadata, String modelId) throws StorageWriteException {
        try {

            storageMetadata.setModelId(modelId);
            setDataDirty();
            if (metadataExists(modelId)) {
                LOG.debug("Updating metadata for " + modelId + " into Hibernate");
                em.merge(storageMetadata);
            } else {
                LOG.debug("Saving metadata for " + modelId + " into Hibernate");
                em.persist(storageMetadata);
            }
        } catch (Exception e) {
            LOG.error("Error saving metadata for model=" + modelId);
            throw new StorageWriteException(e.getMessage());
        }
    }

    // JOINT DATAFRAME AND METADATA READS ==============================================================================
    public Pair<Dataframe, StorageMetadata> readDataframeAndMetadataTagFiltering(String modelId, int batchSize, Set<String> tags, boolean invertFilter) throws StorageReadException {
        if (dataExists(modelId)) {
            Dataframe df;
            try {
                LOG.debug("Reading dataframe " + modelId + " from Hibernate (tagged)");
                refreshIfDirty();
                List<DataframeRow> rows = em.createQuery("" +
                        "select dr from DataframeRow dr" +
                        " where dr.modelId = ?1 AND dr.tag " +
                        (invertFilter ? "not in " : "in ") +
                        "(?2)" +
                        "order by dr.dbId DESC ", DataframeRow.class)
                        .setParameter(1, modelId)
                        .setParameter(2, tags)
                        .setMaxResults(batchSize).getResultList();
                Collections.reverse(rows);
                DataframeMetadata dm = em.find(DataframeMetadata.class, modelId);
                df = Dataframe.untranspose(rows, dm);
            } catch (Exception e) {
                throw new DataframeCreateException(e.getMessage());
            }
            try {
                return Pair.of(df, readMetaOrInternalData(modelId));
            } catch (StorageReadException e) {
                LOG.error(e.getMessage());
                throw new StorageReadException(e.getMessage());
            }
        } else {
            throw new StorageReadException("Error reading dataframe for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    public Pair<Dataframe, StorageMetadata> readDataframeAndMetadataIdFiltering(String modelId, Set<String> ids, boolean invertFilter) throws StorageReadException {
        if (dataExists(modelId)) {
            Dataframe df;
            try {
                LOG.debug("Reading dataframe " + modelId + " from Hibernate (tagged)");
                refreshIfDirty();
                List<DataframeRow> rows = em.createQuery(
                        "select dr from DataframeRow dr" +
                                " where dr.modelId = ?1 AND dr.rowId " +
                                (invertFilter ? "not in " : "in ") +
                                "(?2)" +
                                "order by dr.dbId DESC ",
                        DataframeRow.class)
                        .setParameter(1, modelId)
                        .setParameter(2, ids)
                        .getResultList();
                Collections.reverse(rows);
                DataframeMetadata dm = em.find(DataframeMetadata.class, modelId);
                df = Dataframe.untranspose(rows, dm);
            } catch (Exception e) {
                throw new DataframeCreateException(e.getMessage());
            }
            try {
                return Pair.of(df, readMetaOrInternalData(modelId));
            } catch (StorageReadException e) {
                LOG.error(e.getMessage());
                throw new StorageReadException(e.getMessage());
            }
        } else {
            throw new StorageReadException("Error reading dataframe for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    @Override
    @Transactional
    public Pair<Dataframe, StorageMetadata> readDataframeAndMetadataWithTags(String modelId, int batchSize, Set<String> tags) throws StorageReadException {
        return readDataframeAndMetadataTagFiltering(modelId, batchSize, tags, false);
    }

    @Override
    public Pair<Dataframe, StorageMetadata> readDataframeAndMetadataWithIds(String modelId, Set<String> ids) throws StorageReadException {
        return readDataframeAndMetadataIdFiltering(modelId, ids, false);
    }

    @Override
    public Pair<Dataframe, StorageMetadata> readDataframeAndMetadataWithoutIds(String modelId, Set<String> ids) throws StorageReadException {
        return readDataframeAndMetadataIdFiltering(modelId, ids, true);
    }

    @Override
    @Transactional
    public Pair<Dataframe, StorageMetadata> readDataframeAndMetadataWithTags(String modelId, Set<String> tags) throws StorageReadException {
        return readDataframeAndMetadataTagFiltering(modelId, this.batchSize, tags, false);
    }

    @Override
    @Transactional
    public Pair<Dataframe, StorageMetadata> readDataframeAndMetadataWithoutTags(String modelId, int batchSize, Set<String> tags) throws StorageReadException {
        return readDataframeAndMetadataTagFiltering(modelId, batchSize, tags, true);
    }

    @Override
    @Transactional
    public Pair<Dataframe, StorageMetadata> readDataframeAndMetadataWithoutTags(String modelId, Set<String> tags) throws StorageReadException {
        return readDataframeAndMetadataTagFiltering(modelId, this.batchSize, tags, true);
    }

    // INFERENCE IDS ===================================================================================================
    public List<InferenceId> readInferencesIds(String modelId, Set<String> tags, boolean invertFilter) throws StorageReadException {
        if (dataExists(modelId)) {
            try {
                LOG.debug("Reading inference ids from " + modelId + " from Hibernate (tagged)");
                refreshIfDirty();

                // grab just the id and timestamp as a list of 2 element object arrays
                List<Object[]> objects = em.createQuery("" +
                        "select dr.rowId, dr.timestamp from DataframeRow dr" +
                        " where dr.modelId = ?1 AND dr.tag " +
                        (invertFilter ? "not in " : "in ") + "(?2)" +
                        "order by dr.dbId DESC ")
                        .setParameter(1, modelId)
                        .setParameter(2, tags)
                        .getResultList();

                // unpack tuples returned from db query
                return objects.stream().map(o -> new InferenceId((String) o[0], (LocalDateTime) o[1])).collect(Collectors.toList());
            } catch (StorageReadException e) {
                LOG.error(e.getMessage());
                throw new StorageReadException(e.getMessage());
            }
        } else {
            throw new StorageReadException("Error reading dataframe for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    @Override
    public List<InferenceId> readAllInferenceIds(String modelId) throws StorageReadException {
        if (dataExists(modelId)) {
            try {
                LOG.debug("Reading all inference IDs " + modelId + " from Hibernate ");
                refreshIfDirty();

                // grab just the id and timestamp as a list of 2 element object arrays
                List<Object[]> objects = em.createQuery("" +
                        "select dr.rowId, dr.timestamp from DataframeRow dr" +
                        " where dr.modelId = ?1 " +
                        "order by dr.dbId DESC ")
                        .setParameter(1, modelId)
                        .setMaxResults(batchSize).getResultList();

                // unpack tuples returned from db query
                return objects.stream().map(o -> new InferenceId((String) o[0], (LocalDateTime) o[1])).collect(Collectors.toList());
            } catch (StorageReadException e) {
                LOG.error(e.getMessage());
                throw new StorageReadException(e.getMessage());
            }
        } else {
            throw new StorageReadException("Error reading dataframe for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    @Override
    public List<InferenceId> readAllOrganicInferenceIds(String modelId) throws StorageReadException {
        final Set<String> tags = Set.of(Dataframe.InternalTags.UNLABELED.get());
        return readInferencesIds(modelId, tags, false);

    }

    // TAG MANIPULATION ================================================================================================
    @Transactional
    public void setTags(DataTagging dataTagging) {
        String modelId = dataTagging.getModelId();
        long nrows = rowCount(modelId);
        setDataDirty();
        for (Map.Entry<String, List<List<Integer>>> entry : dataTagging.getDataTagging().entrySet()) {
            for (List<Integer> idxs : entry.getValue()) {
                if (idxs.size() > 2) {
                    throw new IllegalArgumentException("Tag " + entry.getValue() + " keys (" + entry.getKey() + ") contain a sublist with more than two items. Please ensure sublists" +
                            "contain either one element (to indicate a single index) or a pair of elements (to indicate a slice of indices)");
                }

                if (idxs.get(0) >= nrows || (idxs.size() > 1 && idxs.get(1) > nrows)) {
                    throw new IndexOutOfBoundsException("Tag " + entry.getValue() + " sublists contain an out-of-range index: " + idxs + ". Dataframe only has " + nrows + " rows.");
                }

                // if a tuple, assign all points in slice to tag
                if (idxs.size() == 2) {
                    List<DataframeRow> rows = getRowsBetween(modelId, idxs.get(0), idxs.get(1));
                    for (DataframeRow row : rows) {
                        row.setTag(entry.getKey());
                    }
                } else { //otherwise just assign the one provided point
                    DataframeRow row = getRow(modelId, idxs.get(0));
                    row.setTag(entry.getKey());
                }
            }
        }
    }

    @Transactional
    public List<String> getTags(String modelId) {
        if (dataExists(modelId)) {
            refreshIfDirty();
            return em.createQuery(
                    "select dr.tag from DataframeRow dr " +
                            "where dr.modelId = ?1",
                    String.class)
                    .setParameter(1, modelId)
                    .getResultList();
        } else {
            throw new StorageReadException("Error reading tags for model=" + modelId + ": " + NO_DATA_ERROR_MSG);
        }
    }

    // NAME MAPPING MANIPULATION =======================================================================================
    @Transactional
    public void applyNameMapping(NameMapping nameMapping) {
        setDataDirty();
        final StorageMetadata storageMetadata = readMetaOrInternalData(nameMapping.getModelId());
        Schema inputSchema = storageMetadata.getInputSchema();
        Schema outputSchema = storageMetadata.getOutputSchema();
        inputSchema.setNameMapping(nameMapping.getInputMapping());
        outputSchema.setNameMapping(nameMapping.getOutputMapping());

        DataframeMetadata dm = em.find(DataframeMetadata.class, nameMapping.getModelId());
        dm.setNameAliases(storageMetadata.getJointNameAliases());
        dataDirty = true;
    }

    @Transactional
    public void clearNameMapping(String modelId) {
        setDataDirty();
        final StorageMetadata storageMetadata = readMetaOrInternalData(modelId);
        Schema inputSchema = storageMetadata.getInputSchema();
        Schema outputSchema = storageMetadata.getOutputSchema();
        inputSchema.setNameMapping(new HashMap<>());
        outputSchema.setNameMapping(new HashMap<>());

        DataframeMetadata dm = em.find(DataframeMetadata.class, modelId);
        dm.setNameAliases(storageMetadata.getJointNameAliases());
        dataDirty = true;
    }

    // METADATA INFO QUERIES ===========================================================================================
    @Override
    @Transactional
    public boolean dataExists(String modelId) {
        return em.createQuery("" +
                "select 1 from DataframeMetadata dm" +
                " where dm.id = ?1" +
                " order by dm.id limit 1", Boolean.class)
                .setParameter(1, modelId)
                .getResultList().size() > 0;
    }

    @Transactional
    public boolean hasRecordedInferences(String modelId) {
        return em.createQuery("" +
                "select sm.recordedInferences from StorageMetadata sm" +
                " where sm.modelId = ?1", Boolean.class)
                .setParameter(1, modelId)
                .getSingleResult();
    }

    @Transactional
    public boolean metadataExists(String modelId) {
        Long rows = em.createQuery("" +
                "select count(sm) from StorageMetadata sm" +
                " where sm.modelId = ?1", Long.class)
                .setParameter(1, modelId)
                .getSingleResult();
        return rows > 0;
    }

    @Override
    public long getLastModified(String modelId) {
        return updateHash;
    }

    // UPDATE HASHING AND TRACKING =====================================================================================
    public void setDataDirty() {
        dataDirty = true;
        updateHash = rng.nextLong();
    }

    // if a single field of an entity has changed
    public void refreshIfDirty() {
        if (dataDirty) {
            LOG.debug("Refreshing dirty data");
            em.clear();
            dataDirty = false;
        }
    }

    // FIND EXISTING DATA ==============================================================================================
    @Transactional
    public List<String> getAllTrackedDataframes() {
        return em.createQuery("" +
                "select sm.modelId from StorageMetadata sm", String.class).getResultList();
    }

    // TESTING OPERATIONS ==============================================================================================
    @Transactional
    public void clearData(String modelId) {
        LOG.info("Deleting all data from " + modelId + " within database.");
        setDataDirty();
        em.createQuery("" +
                "DELETE from DataframeRow dr " +
                "where dr.modelId = ?1")
                .setParameter(1, modelId)
                .executeUpdate();

        DataframeMetadata dataframeMetadataToDelete = em.find(DataframeMetadata.class, modelId);
        if (dataframeMetadataToDelete != null) {
            em.remove(dataframeMetadataToDelete);
        }

        StorageMetadata storageMetadataToDelete = em.find(StorageMetadata.class, modelId);
        if (storageMetadataToDelete != null) {
            em.remove(storageMetadataToDelete);
        }
    }

}
