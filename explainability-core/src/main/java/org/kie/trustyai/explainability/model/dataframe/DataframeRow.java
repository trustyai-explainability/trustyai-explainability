package org.kie.trustyai.explainability.model.dataframe;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.explainability.model.Value;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

@Entity
public class DataframeRow {

    @ElementCollection
    @OrderColumn(name = "order_column")
    @CollectionTable(name = "DataframeRow_Values")
    private List<Value> row;
    private LocalDateTime timestamp;
    private String rowId;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long dbId;

    private String tag;

    private String modelId;

    public DataframeRow(List<Value> row, LocalDateTime timestamp, String rowId, String tag, String modelId) {
        this.row = new ArrayList<>(row);
        this.timestamp = timestamp;
        this.rowId = rowId;
        this.tag = tag;
        this.modelId = modelId;
    }

    // hibernate setters
    protected DataframeRow() {
    }

    protected void setRow(ArrayList<Value> row) {
        this.row = row;
    }

    protected void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    protected void setRowId(String id) {
        this.rowId = id;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public List<Value> getRow() {
        return row;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getRowId() {
        return rowId;
    }

    public String getTag() {
        return tag;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public Long getDbId() {
        return dbId;
    }

    public void setDbId(Long dbId) {
        this.dbId = dbId;
    }
}
