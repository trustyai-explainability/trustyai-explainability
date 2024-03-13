package org.kie.trustyai.service.data.storage.hibernate;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.service.BaseTestProfile;
import org.kie.trustyai.service.data.storage.flatfile.PVCStorage;
import org.kie.trustyai.service.mocks.MockDatasource;
import org.kie.trustyai.service.mocks.MockPrometheusScheduler;
import org.kie.trustyai.service.mocks.hibernate.MockHibernateStorage;
import org.kie.trustyai.service.mocks.memory.MockMemoryStorage;
import org.kie.trustyai.service.mocks.pvc.MockPVCStorage;
import org.kie.trustyai.service.utils.DataframeGenerators;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.kie.trustyai.service.data.storage.DataFormat.BEAN;
import static org.kie.trustyai.service.data.storage.StorageFormat.HIBERNATE;

public class MigrationTestProfile extends BaseTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("service.storage-format", String.valueOf(HIBERNATE));
        overrides.put("storage.migration.from-folder", "/tmp");
        overrides.put("storage.migration.from-file", "data.csv");
        return overrides;
    }
}
