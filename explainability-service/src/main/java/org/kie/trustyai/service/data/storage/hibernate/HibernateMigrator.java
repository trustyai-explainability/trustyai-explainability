package org.kie.trustyai.service.data.storage.hibernate;

import io.quarkus.arc.lookup.LookupUnlessProperty;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.kie.trustyai.service.config.storage.StorageConfig;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.data.storage.flatfile.PVCStorage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Optional;

@LookupUnlessProperty(name = "storage.storage.migration-config.from-file", stringValue = "")
@ApplicationScoped
public class HibernateMigrator {
    private static final Logger LOG = Logger.getLogger(HibernateMigrator.class);

    @Inject
    Instance<DataSource> dataSource;

    public HibernateMigrator(StorageConfig storageConfig) throws InvalidPropertiesFormatException {
        Optional<String> fromFolder = storageConfig.migrationConfig().fromFolder();
        Optional<String> fromFile = storageConfig.migrationConfig().fromFilename();

        if (fromFile.isPresent() && fromFolder.isPresent()){
            PVCStorage pvcStorage = new PVCStorage(fromFolder.get(), fromFile.get(), -1);
            List<String> modelIds = pvcStorage.listAllModelIds();

            dataSource.get().setStorageOverride(pvcStorage);
            for (String modelId : modelIds) {
                LOG.info("Migrating " + modelId);
                dataSource.get().saveDataframe(dataSource.get().getDataframe(modelId), modelId);
            }
            dataSource.get().clearStorageOverride();
        } else {
            throw new InvalidPropertiesFormatException("Both migration file and folder must be specified to perform database migration.");
        }
    }

}
