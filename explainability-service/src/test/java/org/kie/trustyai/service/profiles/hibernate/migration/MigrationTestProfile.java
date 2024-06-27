package org.kie.trustyai.service.profiles.hibernate.migration;

import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockMigratingHibernateStorage;
import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;

@QuarkusTestResource(H2DatabaseTestResource.class)
public class MigrationTestProfile extends HibernateTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("storage.data-filename", "data.csv");
        overrides.put("storage.data-folder", "/tmp");
        return overrides;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockHibernateDatasource.class, MockMigratingHibernateStorage.class, MockPrometheusScheduler.class);
    }
}
