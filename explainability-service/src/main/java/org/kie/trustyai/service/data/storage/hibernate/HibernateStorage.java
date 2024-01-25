package org.kie.trustyai.service.data.storage.hibernate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.persistence.PersistenceContext;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.exceptions.StorageReadException;
import org.kie.trustyai.service.data.exceptions.StorageWriteException;
import org.kie.trustyai.service.data.metadata.Metadata;
import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.Storage;

import io.quarkus.arc.lookup.LookupIfProperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
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
        List<Integer> filter = IntStream.range(length - batchSize, length).boxed().collect(Collectors.toList());
        return df.filterByRowIndex(filter);
    }

    @Override
    @Transactional
    public void save(Dataframe dataframe, String modelId) throws StorageWriteException {
        dataframe.setId(modelId);
        em.persist(dataframe);
    }

    @Override
    public Metadata readMetadata(String modelId) throws StorageReadException {
        return em.find(Metadata.class, modelId);
    }

    @Override
    @Transactional
    public void saveMetadata(Metadata metadata, String modelId) throws StorageWriteException {
        metadata.setModelId(modelId);
        em.persist(metadata);
    }

    @Transactional
    public void updateMetadata(Metadata metadata) throws StorageWriteException {
        em.merge(metadata);
    }

    @Override
    @Transactional
    public void append(Dataframe dataframe, String modelId) throws StorageWriteException {
        Dataframe original = readData(modelId);
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
