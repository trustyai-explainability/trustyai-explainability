package org.kie.trustyai.service.profiles.flatfile;

import java.util.Map;

public class DisabledEndpointsTestProfile extends MemoryTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("endpoints.data.download", "disable");
        return overrides;
    }

}
