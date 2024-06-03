package org.kie.trustyai.service.profiles.hibernate;

import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockMigratingHibernateStorage;
import org.kie.trustyai.service.mocks.pvc.MockPVCStorage;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;

@QuarkusTestResource(H2DatabaseTestResource.class)
public class BatchedMigrationTestProfile extends MigrationTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("service.batch-size", "50");
        return overrides;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockHibernateDatasource.class, MockPVCStorage.class, MockMigratingHibernateStorage.class, MockPrometheusScheduler.class);
    }
}
