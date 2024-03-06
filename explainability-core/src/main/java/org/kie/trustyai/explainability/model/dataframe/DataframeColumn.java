package org.kie.trustyai.explainability.model.dataframe;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.kie.trustyai.explainability.model.Value;

// hibernate wrapper for an arraylist
//@Entity
public class DataframeColumn {
    private List<Value> values;

    public DataframeColumn(List<Value> values) {
        this.values = values;
    }

    public DataframeColumn() {
        this.values = new ArrayList<>();
    }

    public Value get(int idx) {
        return this.values.get(idx);
    }

    public void add(Value v) {
        this.values.add(v);
    }

    public int size() {
        return this.values.size();
    }

    public void set(int idx, Value v) {
        this.values.set(idx, v);
    }

    public List<Value> getValues() {
        return this.values;
    }

    public List<Value> copyValues() {
        return new ArrayList<>(values);
    }

    public Stream<Value> stream() {
        return this.values.stream();
    }
}
