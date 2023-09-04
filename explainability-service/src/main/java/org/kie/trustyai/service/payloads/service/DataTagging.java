package org.kie.trustyai.service.payloads.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataTagging {
    private final String modelId;
    private final Map<String, List<List<Integer>>> dataTagging;

    public DataTagging(String modelId, Map<String, List<List<Integer>>> dataTagging) {
        this.modelId = modelId;
        this.dataTagging = dataTagging;
    }

    public String getModelId() {
        return modelId;
    }

    public Map<String, List<List<Integer>>> getDataTagging() {
        return dataTagging;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DataTagging that = (DataTagging) o;
        return modelId.equals(that.modelId) && dataTagging.equals(that.dataTagging);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId, dataTagging);
    }
}
