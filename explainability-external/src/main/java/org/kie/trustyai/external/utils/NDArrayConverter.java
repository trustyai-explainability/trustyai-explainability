package org.kie.trustyai.external.utils;

import org.kie.trustyai.explainability.model.Dataframe;

import jep.NDArray;

public class NDArrayConverter<T> {

    private final Dataframe dataframe;

    public NDArrayConverter(Dataframe dataframe) {
        this.dataframe = dataframe;
    }

    public NDArray<T> getData() {
        final double[] data = new double[dataframe.getRowDimension() * dataframe.getColumnDimension()];
        for (int i = 0; i < dataframe.getRowDimension(); i++) {
            for (int j = 0; j < dataframe.getColumnDimension(); j++) {
                data[i * dataframe.getColumnDimension() + j] = dataframe.getValue(i, j).asNumber();
            }
        }
        return new NDArray<T>((T) data, dataframe.getRowDimension(), dataframe.getColumnDimension());
    }

}
