package org.kie.trustyai.service.profiles.hibernate;

import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockMigratingHibernateStorage;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;

@QuarkusTestResource(H2DatabaseTestResource.class)
public class MigrationTestProfile extends HibernateTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("storage.migration-config.from-folder", "/tmp");
        overrides.put("storage.migration-config.from-filename", "data.csv");
        return overrides;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockDatasource.class, MockMigratingHibernateStorage.class, MockPrometheusScheduler.class);
    }
}
