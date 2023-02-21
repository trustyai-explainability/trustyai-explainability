package org.kie.trustyai.service.scenarios.batching;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.kie.trustyai.service.endpoints.consumer.MockConsumerDatasource;

import io.quarkus.test.junit.QuarkusTestProfile;

public class BatchingTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        final Map<String, String> overrides = new HashMap<>();
        overrides.put("service.storage-format", "PVC");
        overrides.put("service.model-name", "example");
        overrides.put("service.data-format", "CSV");
        overrides.put("service.metrics-schedule", "5s");
        overrides.put("service.batch-size", "50");
        overrides.put("pvc.input-filename", "input.csv");
        overrides.put("pvc.output-filename", "output.csv");
        return overrides;
    }

    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(MockConsumerDatasource.class);

    }

}