package org.kie.trustyai.explainability.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OrderColumn;

// hibernate wrapper for an arraylist
@Entity
public class DataframeColumn {
    @ElementCollection
    @OrderColumn(name = "order_column")
    private List<Value> values;

    @Id
    @GeneratedValue
    private Long id;

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

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
