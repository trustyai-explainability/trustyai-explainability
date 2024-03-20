package org.kie.trustyai.explainability.model;

import java.io.Serializable;
import java.util.Objects;

public class UnderlyingObject implements Serializable {
    private Object object;

    public UnderlyingObject() {

    }

    public UnderlyingObject(Object o) {
        object = o;
    }

    public Object getObject() {
        return this.object;
    }

    public void setObject(Object o) {
        this.object = o;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UnderlyingObject that = (UnderlyingObject) o;
        return Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }

}
