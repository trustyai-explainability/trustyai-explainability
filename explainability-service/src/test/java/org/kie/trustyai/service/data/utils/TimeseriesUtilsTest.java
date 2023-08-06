package org.kie.trustyai.service.data.utils;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.PredictionInput;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeseriesUtilsTest {

    @Test
    void testTransformData() {

        final Map<String, List<Double>> data = Map.of("a", List.of(1.0, 2.0, 3.0), "b", List.of(4.0, 5.0, 6.0));
        final List<Object> timestamps = List.of("2020-01-01", "2020-01-02", "2020-01-03");
        final String timestampName = "timestamp";

        final List<PredictionInput> result = TimeseriesUtils.transformData(data, timestamps, timestampName);

        final Dataframe dataframe = Dataframe.createFromInputs(result);

        assertEquals(3, dataframe.getColumnDimension());
        assertEquals(3, dataframe.getColumnDimension());

    }

}