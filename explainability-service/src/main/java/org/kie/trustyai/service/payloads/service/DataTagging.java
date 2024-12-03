package org.kie.trustyai.service.payloads.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataTagging {
    private final String modelId;
    private final Map<String, List<List<Integer>>> dataTagging;

    /**
     * Create a DataTagging
     *
     * @param modelId the modelId to apply this tagging to
     * @param dataTagging the data tagging map. This is structured as follows:
     *        Each item in the map assigns a tag to a set of matching dataframe rows. The tag is indicated
     *        by the map key, while the values are a list of lists of integers, which indicate which
     *        rows to select.
     *
     *        Each list of integers can either be a single item, which will select just that one index, or
     *        a tuple of integers, which will select the range of indices [a, b). For example:"
     *
     *        `tagA : [0], [2, 5], [7], [10-15]`
     *
     *        would apply "tagA" to the following rows:
     *        - 0
     *        - 2, 3, 4
     *        - 7
     *        - 10, 11, 12, 13, 14
     */
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
