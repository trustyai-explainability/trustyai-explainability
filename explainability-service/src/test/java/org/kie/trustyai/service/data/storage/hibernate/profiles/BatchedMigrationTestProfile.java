package org.kie.trustyai.service.data.storage.hibernate.profiles;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockMigratingHibernateStorage;

import java.util.Map;
import java.util.Set;

import static org.kie.trustyai.service.data.storage.DataFormat.BEAN;
import static org.kie.trustyai.service.data.storage.StorageFormat.HIBERNATE;

@QuarkusTestResource(H2DatabaseTestResource.class)
public class BatchedMigrationTestProfile extends BaseTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("service.storage-format", String.valueOf(HIBERNATE));
        overrides.put("service.data-format", String.valueOf(BEAN));
        overrides.put("storage.migration-config.from-folder", "/tmp");
        overrides.put("storage.migration-config.from-filename", "data.csv");
        overrides.put("service.batch-size", "50");
        return overrides;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockDatasource.class, MockMigratingHibernateStorage.class, MockPrometheusScheduler.class);
    }
}
