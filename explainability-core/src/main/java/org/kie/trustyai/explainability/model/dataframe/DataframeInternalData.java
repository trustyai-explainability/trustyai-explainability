package org.kie.trustyai.explainability.model.dataframe;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataframeInternalData {
    private final List<String> datapointTags;
    private final List<String> ids;
    private final List<LocalDateTime> timestamps;
    private Integer size;

    public DataframeInternalData() {
        this.datapointTags = new ArrayList<>();
        this.ids = new ArrayList<>();
        this.timestamps = new ArrayList<>();
        this.size = 0;
    }

    public DataframeInternalData(List<String> datapointTags, List<String> ids, List<LocalDateTime> timestamps) {
        this.datapointTags = datapointTags;
        this.ids = ids;
        this.timestamps = timestamps;
        this.size = ids.size();
    }

    DataframeInternalData copy() {
        return new DataframeInternalData(new ArrayList<>(this.datapointTags), new ArrayList<>(this.ids),
                new ArrayList<>(this.timestamps));
    }

    public int size() {
        return this.size;
    }

    public List<String> getDatapointTags() {
        return datapointTags;
    }

    public List<String> getIds() {
        return Collections.unmodifiableList(ids);
    }

    public List<LocalDateTime> getTimestamps() {
        return Collections.unmodifiableList(timestamps);
    }

    public Integer getSize() {
        return size;
    }

    public void incrementSize() {
        size++;
    }

    public void addDatapointTag(String tag) {
        datapointTags.add(tag);
    }

    public void addId(String id) {
        ids.add(id);
    }

    public void addTimestamp(LocalDateTime dt) {
        timestamps.add(dt);
    }

    public void setDatapointTag(int idx, String tag) {
        datapointTags.set(idx, tag);
    }

    public void setId(int idx, String id) {
        ids.set(idx, id);
    }

    public void setTimestamp(int idx, LocalDateTime dt) {
        timestamps.set(idx, dt);
    }
}
