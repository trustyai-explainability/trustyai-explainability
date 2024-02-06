package org.kie.trustyai.service.data.storage.hibernate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.persistence.TypedQuery;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.DataframeColumn;
import org.kie.trustyai.explainability.model.DataframeInternalData;
import org.kie.trustyai.explainability.model.DataframeMetadata;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.Storage;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.transaction.Transactional;

import javax.xml.crypto.Data;

@LookupIfProperty(name = "service.storage.format", stringValue = "HIBERNATE")
@ApplicationScoped
public class HibernateStorage extends Storage implements HibernateStorageInterface {
    @PersistenceContext
    EntityManager em;

    private static final Logger LOG = Logger.getLogger(HibernateStorage.class);
    private final int batchSize;

    public HibernateStorage(ServiceConfig serviceConfig, StorageConfig storageConfig) {
        LOG.info("Starting Hibernate storage consumer");

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
        return DataFormat.BEAN;
    }

    @Override
    public Dataframe readData(String modelId) throws StorageReadException {
        LOG.debug("Reading dataframe " + modelId + " from Hibernate");
        return readData(modelId, batchSize);
    }

    public Dataframe readAllData(String modelId) throws StorageReadException {
        LOG.debug("Reading dataframe " + modelId + " from Hibernate");
        return em.find(Dataframe.class, modelId);
    }

    // get the row count of a persisted dataframe
    public int rowCount(String modelId){
        return em.
                createQuery("select df.internalData.size from Dataframe df where df.id = ?1", int.class)
                .setParameter(1, modelId)
                .getSingleResult();
    }

    public int colCount(String modelId){
        return (int) em.createQuery("select size(df.data) from Dataframe df WHERE df.id = ?1")
                .setParameter(1, modelId)
                .getSingleResult();
    }

    @Override
    public Dataframe readData(String modelId, int batchSize) throws StorageReadException {
        LOG.debug("Reading dataframe " + modelId + " from Hibernate (batched)");

        int rowCount = rowCount(modelId);
        int colCount = colCount(modelId);
        int startIdx = Math.max(0, rowCount- batchSize);
        int endIdx = Math.min(rowCount, startIdx + batchSize);


        List<List<Value>> batchColumns = new ArrayList<>();
        for (int colIdx=0; colIdx<colCount; colIdx++){
            List<Value> colValues = em.createQuery(
                            "" +
                                    "select v from Dataframe df " +
                                    "JOIN df.data d JOIN d.values v " +
                                    "WHERE df.id = ?1 " +
                                    "AND index(d) = ?2 " +
                                    "AND index(v) >= ?3 AND index(v) < ?4 ",
                            Value.class
                    )
                    .setParameter(1, modelId)
                    .setParameter(2, colIdx)
                    .setParameter(3, startIdx)
                    .setParameter(4, endIdx)
                    .getResultList();
            batchColumns.add(colValues);
        }

        List<LocalDateTime> timestamps = em.createQuery("select ts from Dataframe df " +
                                "JOIN df.internalData internal JOIN internal.timestamps ts " +
                                "WHERE df.id = ?1 " +
                                "AND index(ts) >= ?2 and INDEX(ts) < ?3",
                        LocalDateTime.class
                )
                .setParameter(1, modelId)
                .setParameter(2, startIdx)
                .setParameter(3, endIdx)
                .getResultList();
        List<String> ids = em.createQuery("select ids from Dataframe df " +
                                "JOIN df.internalData internal JOIN internal.ids ids " +
                                "WHERE df.id = ?1 " +
                                "AND index(ids) >= ?2 and INDEX(ids) < ?3",
                        String.class
                )
                .setParameter(1, modelId)
                .setParameter(2, startIdx)
                .setParameter(3, endIdx)
                .getResultList();
        List<String> datapointTags = em.createQuery("select tags from Dataframe df " +
                                "JOIN df.internalData internal JOIN internal.datapointTags tags " +
                                "WHERE df.id = ?1 " +
                                "AND index(tags) >= ?2 and INDEX(tags) < ?3",
                        String.class
                )
                .setParameter(1, modelId)
                .setParameter(2, startIdx)
                .setParameter(3, endIdx)
                .getResultList();
        DataframeInternalData dfInternalData = new DataframeInternalData(datapointTags, ids, timestamps);
        DataframeMetadata dfMetadata = em.createQuery("select df.metadata from Dataframe df WHERE df.id = ?1", DataframeMetadata.class)
                .setParameter(1, modelId).getSingleResult();

        return new Dataframe(batchColumns, dfMetadata, dfInternalData);
    }

    @Override
    @Transactional
    public void save(Dataframe dataframe, String modelId) throws StorageWriteException {
        LOG.debug("Writing dataframe=" + modelId + ", rows=" + dataframe.getRowDimension() + " to Hibernate");
        dataframe.setId(modelId);
        em.persist(dataframe);
    }

    @Override
    public StorageMetadata readMetadata(String modelId) throws StorageReadException {
        LOG.debug("Reading metadata for " + modelId + " from Hibernate");
        return em.find(StorageMetadata.class, modelId);
    }

    @Override
    @Transactional
    public void saveMetadata(StorageMetadata storageMetadata, String modelId) throws StorageWriteException {
        LOG.debug("Saving metadata for " + modelId + " into Hibernate");
        storageMetadata.setModelId(modelId);
        em.persist(storageMetadata);
    }

    @Transactional
    public void updateMetadata(StorageMetadata storageMetadata) throws StorageWriteException {
        LOG.debug("Updating existing metadata for " + storageMetadata.getModelId() + " in Hibernate");
        em.merge(storageMetadata);
    }

    @Override
    @Transactional
    public void append(Dataframe dataframe, String modelId) throws StorageWriteException {
        LOG.debug("Appending " + dataframe.getRowDimension() + " new rows to dataframe=" + modelId + " within Hibernate");
        Dataframe original = readData(modelId);
        original.setId(modelId);
        original.addPredictions(dataframe.asPredictions());
        em.merge(original);
    }

    @Override
    public boolean dataframeExists(String modelId) throws StorageReadException {
        try {
            return em.find(Dataframe.class, modelId) != null;
        } catch (EntityNotFoundException e) {
            return false;
        }
    }

    @Override
    public long getLastModified(String modelId) {
        return 0;
    }
}
