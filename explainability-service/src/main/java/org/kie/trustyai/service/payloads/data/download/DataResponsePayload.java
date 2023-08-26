package org.kie.trustyai.service.payloads.data.download;

public class DataResponsePayload {
    String dataCSV;

    public String getDataCSV() {
        return dataCSV;
    }

    public void setDataCSV(String dataCSV) {
        this.dataCSV = dataCSV;
    }

    @Override
    public String toString() {
        return "DataResponsePayload{" +
                "dataCSV='" + dataCSV + '\'' +
                '}';
    }
}
