package org.kie.trustyai.service.profiles.hibernate;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;
import io.quarkus.test.junit.QuarkusTestProfile;

import static org.kie.trustyai.service.data.storage.StorageFormat.HIBERNATE;

@QuarkusTestResource(H2DatabaseTestResource.class)
public class HibernateTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = new HashMap<>();
        overrides.put("service.storage-format", String.valueOf(HIBERNATE));
        overrides.put("service.data-format", String.valueOf(DataFormat.BEAN));
        overrides.put("quarkus.hibernate-orm.active", "true");
        overrides.put("service.metrics-schedule", "5s");
        overrides.put("service.batch-size", "5000");

        // needed for validation
        overrides.put("storage.data-filename", "data.csv");
        overrides.put("storage.data-folder", "/inputs");

        // hibernate args
        overrides.put("quarkus.datasource.jdbc.url", "jdbc:h2:file:./trustyai_test_H2_DB");
        overrides.put("quarkus.hibernate-orm.database.generation", "drop-and-create");

        return overrides;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockPrometheusScheduler.class);
    }

}
