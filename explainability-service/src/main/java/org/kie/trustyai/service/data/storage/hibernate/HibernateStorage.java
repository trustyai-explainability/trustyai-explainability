package org.kie.trustyai.service.data.storage.hibernate;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
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
import jakarta.transaction.Transactional;

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
        return readData(modelId, batchSize);
    }

    @Override
    public Dataframe readData(String modelId, int batchSize) throws StorageReadException {
        Dataframe df = em.find(Dataframe.class, modelId);
        int length = df.getRowDimension();
        int startIdx = Math.max(0, length - batchSize);
        int endIdx = Math.min(length, batchSize);
        LOG.info("dataframe cols: " + df.getColumnDimension());
        LOG.info("name aliases: "  + df.getNameAliases());
        LOG.info(df);
        LOG.info("start: "+startIdx + " end: "+endIdx);
        List<Integer> filter = IntStream.range(startIdx, endIdx).boxed().collect(Collectors.toList());
        return df.filterByRowIndex(filter);
    }

    @Override
    @Transactional
    public void save(Dataframe dataframe, String modelId) throws StorageWriteException {
        LOG.info("saving dataframe with "+dataframe.getRowDimension() + " rows");
        dataframe.setId(modelId);
        LOG.info("saving metadata aliases: " + dataframe.getNameAliases());
        em.persist(dataframe);
    }

    @Override
    public StorageMetadata readMetadata(String modelId) throws StorageReadException {
        return em.find(StorageMetadata.class, modelId);
    }

    @Override
    @Transactional
    public void saveMetadata(StorageMetadata storageMetadata, String modelId) throws StorageWriteException {
        LOG.info("saving metadata");
        storageMetadata.setModelId(modelId);
        em.persist(storageMetadata);
        LOG.info("metadata persisted");
    }

    @Transactional
    public void updateMetadata(StorageMetadata storageMetadata) throws StorageWriteException {
        LOG.info("updating metadata");
        em.merge(storageMetadata);
    }

    @Override
    @Transactional
    public void append(Dataframe dataframe, String modelId) throws StorageWriteException {
        LOG.info("appending dataframe");
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
