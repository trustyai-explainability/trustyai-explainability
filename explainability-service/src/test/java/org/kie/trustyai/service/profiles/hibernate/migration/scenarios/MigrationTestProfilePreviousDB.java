package org.kie.trustyai.service.profiles.hibernate.migration.scenarios;

import java.util.Map;

import org.kie.trustyai.service.profiles.hibernate.HibernateTestProfile;

public class MigrationTestProfilePreviousDB extends HibernateTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("quarkus.hibernate-orm.database.generation", "update");
        return overrides;
    }
}
