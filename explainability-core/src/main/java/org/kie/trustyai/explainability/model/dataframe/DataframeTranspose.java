package org.kie.trustyai.explainability.model.dataframe;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.explainability.model.Value;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;

@Entity
public class DataframeTranspose {

    @OneToMany(cascade = CascadeType.ALL)
    @OrderColumn(name = "row_order")
    List<DataframeRow> rows;

    @OneToOne(cascade = CascadeType.ALL)
    private final DataframeMetadata metadata;

    private int rowDimension;
    private int colDimension;

    @Id
    private String id;

    public DataframeTranspose() {
        this.rows = new ArrayList<>();
        this.metadata = new DataframeMetadata();
    }

    public DataframeTranspose(List<DataframeRow> rows, DataframeMetadata metadata, String id, int rowDimension, int colDimension) {
        this.rows = rows;
        this.metadata = metadata;
        this.rowDimension = rowDimension;
        this.colDimension = colDimension;
        this.id = id;
    }

    public Dataframe transpose() {
        List<List<Value>> values = new ArrayList<>(colDimension);

        // internal data fields
        List<String> tags = new ArrayList<>(rowDimension);
        List<String> ids = new ArrayList<>(rowDimension);
        List<LocalDateTime> timestamps = new ArrayList<>(rowDimension);

        long loopStart = System.currentTimeMillis();
        for (int c = 0; c < colDimension; c++) {
            List<Value> columnValues = new ArrayList<>(rowDimension);

            for (DataframeRow row : rows) {
                columnValues.add(row.getRow().get(c));

                // grab internal data (only need to do this once)
                if (c == 0) {
                    tags.add(row.getTag());
                    ids.add(row.getRowId());
                    timestamps.add(row.getTimestamp());
                }
            }
            values.add(columnValues);
        }
        long loopEnd = System.currentTimeMillis();
        System.out.println("transpose loop: " + (loopEnd - loopStart));
        return new Dataframe(values, metadata, new DataframeInternalData(tags, ids, timestamps));
    }
}
