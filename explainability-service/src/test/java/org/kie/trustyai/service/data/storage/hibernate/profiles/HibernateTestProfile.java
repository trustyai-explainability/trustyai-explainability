package org.kie.trustyai.service.data.storage.hibernate.profiles;

import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.h2.H2DatabaseTestResource;

import static org.kie.trustyai.service.data.storage.StorageFormat.HIBERNATE;

@QuarkusTestResource(H2DatabaseTestResource.class)
public class HibernateTestProfile extends BaseTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("service.storage-format", String.valueOf(HIBERNATE));
        overrides.put("service.data-format", String.valueOf(DataFormat.BEAN));
        return overrides;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockPrometheusScheduler.class);
    }

}
