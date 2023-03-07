package org.kie.trustyai.service.data.storage;

import java.util.Map;

import org.kie.trustyai.service.BaseTestProfile;

import static org.kie.trustyai.service.data.storage.StorageFormat.PVC;

public class PVCTestProfile extends BaseTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = super.getConfigOverrides();
        overrides.put("service.storage-format", String.valueOf(PVC));
        overrides.put("storage.data-folder", "/tmp");
        return overrides;
    }

}