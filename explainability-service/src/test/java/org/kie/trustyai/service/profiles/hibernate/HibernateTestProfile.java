package org.kie.trustyai.service.profiles.hibernate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateDatasource;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTestProfile;

import static org.kie.trustyai.service.data.storage.StorageFormat.DATABASE;

@QuarkusTestResource(H2DatabaseTestResource.class)
public class HibernateTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = new HashMap<>();
        overrides.put("service.storage-format", String.valueOf(DATABASE));
        overrides.put("quarkus.hibernate-orm.active", "true");
        overrides.put("service.metrics-schedule", "5s");
        overrides.put("service.batch-size", "5000");

        overrides.put("quarkus.datasource.db-kind", "h2");
        overrides.put("quarkus.datasource.jdbc.url", "jdbc:h2:./trustyai_test_H2_DB;AUTO_RECONNECT=TRUE");
        overrides.put("quarkus.hibernate-orm.database.generation", "drop-and-create");

        // these settings are useful for testing against a real Maria instance on a remote cluster, and are therefore kept
        //        overrides.put("quarkus.datasource.db-kind", "mariadb");
        //        overrides.put("quarkus.datasource.username", "quarkus");
        //        overrides.put("quarkus.datasource.password", "qBfi86/5>TtlC0Iw");
        //        overrides.put("quarkus.datasource.jdbc.url", "jdbc:mariadb://mariadb:3306/trustyai_database");//?rewriteBatchedStatements=TRUE");
        //        overrides.put("quarkus.hibernate-orm.database.generation", "drop-and-create");

        return overrides;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockHibernateDatasource.class, MockHibernateStorage.class, MockPrometheusScheduler.class);
    }

}
