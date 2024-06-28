package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;

import org.kie.trustyai.connectors.kserve.KServeDatatype;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class KServeV2ResponsePayload {

    @JsonProperty("model_name")
    public String modelName;
    @JsonProperty("model_version")
    public String modelVersion;
    public String id;
    private List<Outputs> outputs;

    public List<Outputs> getOutputs() {
        return outputs;
    }

    public void setOutputs(List<Outputs> outputs) {
        this.outputs = outputs;
    }

    @Override
    public String toString() {
        return "KServeV1ResponsePayload{" +
                "outputs=" + outputs +
                '}';
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Outputs {
        public String name;
        public List<Integer> shape;
        public KServeDatatype datatype;
        public List<Object> data;

        @Override
        public String toString() {
            return "Outputs{" +
                    "name='" + name + '\'' +
                    ", shape=" + shape +
                    ", datatype=" + datatype +
                    ", data=" + data +
                    '}';
        }
    }
}
