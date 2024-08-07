package org.kie.trustyai.explainability.model;

import java.io.Serializable;
import java.util.Objects;

public class SerializableObject implements Serializable {
    private Object object;

    public SerializableObject() {
        object = null;
    }

    public SerializableObject(Object o) {
        object = o;
    }

    // primitive constructors ==========================================================================================
    // these are to let Jackson deserialize JSON into UnderlyingObjects
    // and bypass the serializable check for known serializable types
    public SerializableObject(byte o) {
        object = o;
    }

    public SerializableObject(Byte o) {
        object = o;
    }

    public SerializableObject(char o) {
        object = o;
    }

    public SerializableObject(Character o) {
        object = o;
    }

    public SerializableObject(short o) {
        object = o;
    }

    public SerializableObject(Short o) {
        object = o;
    }

    public SerializableObject(Integer o) {
        object = o;
    }

    public SerializableObject(int o) {
        object = o;
    }

    public SerializableObject(Long o) {
        object = o;
    }

    public SerializableObject(long o) {
        object = o;
    }

    public SerializableObject(Float o) {
        object = o;
    }

    public SerializableObject(float o) {
        object = o;
    }

    public SerializableObject(Double o) {
        object = o;
    }

    public SerializableObject(double o) {
        object = o;
    }

    public SerializableObject(String o) {
        object = o;
    }

    public SerializableObject(boolean o) {
        object = o;
    }

    public SerializableObject(Boolean o) {
        object = o;
    }

    // getters and setters =============================================================================================
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
        SerializableObject that = (SerializableObject) o;
        return Objects.equals(object, that.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(object);
    }

}
