package org.kie.trustyai.external.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Value;

import jep.SubInterpreter;
import jep.python.PyCallable;
import jep.python.PyObject;

public class Converter {

    private Converter() {
        // Intentionally left blank
    }

    public static PyObject arrayToDataframe(Dataframe dataframe, SubInterpreter interpreter) {
        interpreter.exec("import trustyaiexternal.api as api");
        final NDArrayConverter<double[]> array = new NDArrayConverter<>(dataframe);
        PyCallable callable = (PyCallable) interpreter.getValue("api.Converter.dataframe_from_array");
        return (PyObject) callable.call(array.getData(), dataframe.getColumnNames());
    }

    public static PyObject mapToDataframe(Dataframe dataframe, SubInterpreter interpreter) {
        interpreter.exec("import trustyaiexternal.api as api");
        final Map<String, List<Object>> map = new HashMap<>();
        final int columnDimension = dataframe.getColumnDimension();
        final List<String> columnNames = dataframe.getColumnNames();
        for (int i = 0; i < columnDimension; i++) {
            // Get the column data
            final List<Object> columnData = dataframe.getColumn(i).stream().map(d -> (Object) d).collect(java.util.stream.Collectors.toList());

            // Store the column name and data in the map
            map.put(columnNames.get(i), columnData);
        }
        PyCallable callable = (PyCallable) interpreter.getValue("api.Converter.dataframe_from_map");
        return (PyObject) callable.call(map);
    }

    public static PyObject dataframeToTsframe(Dataframe dataframe, String timestampColumn, String format, SubInterpreter sub) {
        sub.exec("import trustyaiexternal.api as api");

        final Map<String, Object[]> map = new HashMap<>();
        final int columnDimension = dataframe.getColumnDimension();
        final List<String> columnNames = dataframe.getColumnNames();
        for (int i = 0; i < columnDimension; i++) {
            // Get the column data
            final Object[] columnData = dataframe.getColumn(i).stream().map(Value::getUnderlyingObject).toArray();

            // Store the column name and data in the map
            map.put(columnNames.get(i), columnData);
        }
        final PyCallable callable = (PyCallable) sub.getValue("api.Converter.tsframe_from_dataframe");
        return (PyObject) callable.call(map, timestampColumn, format);
    }
}
