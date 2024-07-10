package org.kie.trustyai.service.data.storage.hibernate.migration;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.config.CustomServiceConfig;
import org.kie.trustyai.service.config.ServiceConfig;
import org.kie.trustyai.service.config.storage.CustomStorageConfig;
import org.kie.trustyai.service.config.storage.MigrationConfig;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.datasources.CSVDataSource;
import org.kie.trustyai.service.data.metadata.StorageMetadata;
import org.kie.trustyai.service.data.parsers.CSVParser;
import org.kie.trustyai.service.data.storage.flatfile.PVCStorage;
import org.kie.trustyai.service.data.storage.hibernate.HibernateStorage;

import io.quarkus.narayana.jta.runtime.TransactionConfiguration;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;

@Singleton
public class HibernateMigrator {
    private final int batchSize;
    private Optional<MigrationConfig> migrationConfig = Optional.empty();
    private static final Logger LOG = Logger.getLogger(HibernateMigrator.class);

    @Inject
    Instance<HibernateStorage> storage;

    @Inject
    Event<MigrationEvent.MigrationSingleDataframeFinishEvent> migrationSingleDataframeFinishEvent;

    @Inject
    Event<MigrationEvent.MigrationFinishEvent> migrationFinishEvent;

    public HibernateMigrator(ServiceConfig serviceConfig, StorageConfig storageConfig) {
        if (serviceConfig.batchSize().isPresent()) {
            this.batchSize = serviceConfig.batchSize().getAsInt();
        } else {
            final String message = "Missing data batch size";
            LOG.error(message);
            throw new IllegalArgumentException(message);
        }

        if (storageConfig.dataFilename().isPresent() && storageConfig.dataFolder().isPresent()) {
            migrationConfig = Optional.of(new MigrationConfig(storageConfig.dataFolder().get(), storageConfig.dataFilename().get()));
        }
    }

    public void migrate(@ObservesAsync MigrationEvent.MigrationTriggerEvent event) {
        List<String> modelIds = new ArrayList<>();
        if (migrationConfig.isPresent()) {
            LOG.info("Starting Hibernate migration handler.");
            MigrationConfig mc = migrationConfig.get();
            String fromFolder = mc.getFromFolder();
            String fromFile = mc.getFromFilename();

            if (!Paths.get(fromFolder).toFile().isDirectory()) {
                LOG.warn("A storage folder \"" + fromFolder + "\" was provided in the service configuration, but no such directory exists. Migration will be skipped.");
                return;
            }

            CustomStorageConfig customStorageConfig = new CustomStorageConfig(fromFile, fromFolder);
            CustomServiceConfig customServiceConfig = new CustomServiceConfig(OptionalInt.of(batchSize), null, null);
            PVCStorage pvcStorage = new PVCStorage(customServiceConfig, customStorageConfig);
            CSVDataSource oldDataSource = new CSVDataSource();
            oldDataSource.setParser(new CSVParser());
            oldDataSource.setStorageOverride(pvcStorage);
            modelIds = pvcStorage.listAllModelIds();

            if (!modelIds.isEmpty()) {
                LOG.info("Starting migration, found " + modelIds.size() + " models.");
                for (String modelId : modelIds) {
                    migrateDataframe(modelId, oldDataSource, batchSize);
                }
                LOG.info("Migration complete, the PVC is now safe to remove.");
            } else {
                LOG.info("No models found, no migration to perform.");
            }
            migrationConfig = Optional.empty();
            storage.get().setDataDirty();
        }
        migrationFinishEvent.fire(MigrationEvent.getMigrationFinishEvent());
    }

    @Transactional
    @TransactionConfiguration(timeout = 300)
    protected void migrateDataframe(String modelId, CSVDataSource oldDataSource, int batchSize) {
        LOG.info("Migrating " + modelId + " metadata");
        StorageMetadata sm = oldDataSource.getMetadata(modelId);
        storage.get().saveMetaOrInternalData(sm, modelId);

        // batch save the df
        int nObs = sm.getObservations();
        int startIdx = 0;
        while (startIdx < nObs) {
            int endIdx = Math.min(startIdx + batchSize, nObs);
            LOG.info("Migrating " + modelId + " data, rows " + startIdx + "-" + endIdx + " of " + nObs);
            Dataframe df = oldDataSource.getDataframe(modelId, startIdx, endIdx);
            storage.get().saveDataframe(df, modelId);
            startIdx += batchSize;
        }
        LOG.info("Migration of " + modelId + " is complete, " + storage.get().rowCount(modelId) + " total rows migrated.");
        migrationSingleDataframeFinishEvent.fire(MigrationEvent.getMigrationSingleDataframeFinishEvent(modelId));
    }
}
