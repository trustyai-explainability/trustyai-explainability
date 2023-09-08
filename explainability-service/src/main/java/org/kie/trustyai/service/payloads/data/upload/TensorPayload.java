package org.kie.trustyai.service.payloads.data.upload;

import java.util.Arrays;
import java.util.HashMap;

public class TensorPayload {
    private String name;
    private Number[] shape;
    private String datatype;
    private HashMap<String, Object> parameters;
    private Object[] data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Number[] getShape() {
        return shape;
    }

    public void setShape(Number[] shape) {
        this.shape = shape;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public HashMap<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(HashMap<String, Object> parameters) {
        this.parameters = parameters;
    }

    public Object[] getData() {
        return data;
    }

    public void setData(Object[] data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "ModelInferRequestPayload{" +
                "name='" + name + '\'' +
                ", shape=" + Arrays.toString(shape) +
                ", datatype='" + datatype + '\'' +
                ", parameters=" + parameters +
                ", data=" + Arrays.deepToString(data) +
                '}';
    }
}
