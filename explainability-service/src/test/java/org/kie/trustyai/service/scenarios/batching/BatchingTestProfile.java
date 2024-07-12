package org.kie.trustyai.service.scenarios.batching;

import java.util.Map;

import org.kie.trustyai.service.profiles.flatfile.MemoryTestProfile;

public class BatchingTestProfile extends MemoryTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("service.batch-size", "50");
        return overrides;
    }

}