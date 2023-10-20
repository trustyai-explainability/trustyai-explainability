package org.kie.trustyai.service.data.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.service.payloads.service.Schema;
import org.kie.trustyai.service.payloads.service.SchemaItem;
import org.kie.trustyai.service.payloads.values.DataType;

public class MetadataUtils {
    private static final Logger LOG = Logger.getLogger(KServeInferencePayloadReconciler.class);

    public static int UNIQUE_COLUMN_VALUES_LIMIT = 200;
    public static int MAX_COLUMNS_TO_ENUMERATE = 100;

    private MetadataUtils() {
    }

    private static DataType inferType(Value value) {
        Object o = value.getUnderlyingObject();
        if (o instanceof Integer) {
            return DataType.INT32;
        } else if (o instanceof Double) {
            return DataType.DOUBLE;
        } else if (o instanceof Float) {
            return DataType.FLOAT;
        } else if (o instanceof Long) {
            return DataType.INT64;
        } else if (o instanceof Boolean) {
            return DataType.BOOL;
        } else if (o instanceof String) {
            return DataType.STRING;
        } else if (o instanceof Map) {
            return DataType.MAP;
        } else {
            return DataType.UNKNOWN;
        }
    }

    private static SchemaItem populateSchemaItem(String name, int i, Set<Object> values, DataType dataType) {
        final SchemaItem schemaItem = new SchemaItem();
        schemaItem.setType(dataType);
        schemaItem.setName(name);
        schemaItem.setIndex(i);
        schemaItem.setValues(values);
        return schemaItem;
    }

    // infer datatype, do not get unique value enumeration
    private static SchemaItem extractRowSchemaNoUniquesNoDatatype(Dataframe dataframe, int i, String name) {
        final Value value = dataframe.getValue(0, i);
        return populateSchemaItem(name, i, null, MetadataUtils.inferType(value));

    }

    // use known datatype, do not get unique value enumeration
    private static SchemaItem extractRowSchemaNoUniquesWithDatatype(int i, DataType dataType, String name) {
        return populateSchemaItem(name, i, null, dataType);
    }

    // infer datatype, get unique value enumeration
    private static SchemaItem extractRowSchemaUniquesNoDatatype(Dataframe dataframe, int i, String name) {
        Optional<Set<Object>> uniqueValues = getUniqueValuesShortCircuited(dataframe.getColumn(i));
        final Value value = dataframe.getValue(0, i);
        return populateSchemaItem(name, i, uniqueValues.orElse(null), MetadataUtils.inferType(value));
    }

    // use known datatype, get unique value enumeration
    private static SchemaItem extractRowSchemaUniquesWithDatatype(Dataframe dataframe, int i, DataType dataType, String name) {
        Optional<Set<Object>> uniqueValues = getUniqueValuesShortCircuited(dataframe.getColumn(i));
        return populateSchemaItem(name, i, uniqueValues.orElse(null), dataType);
    }

    private static Optional<Set<Object>> getUniqueValuesShortCircuited(List<Value> column) {
        Set<Object> uniqueValues = new HashSet<>();
        int nUniques = 0;
        for (Value v : column) {
            if (uniqueValues.add(v.getUnderlyingObject())) {
                nUniques += 1;
                if (nUniques > MetadataUtils.UNIQUE_COLUMN_VALUES_LIMIT) {
                    return Optional.empty();
                }
            }
        }
        return Optional.of(uniqueValues);
    }

    // use specialized function for map inner loop, depending on necessary computations
    private static Schema getGenericSchema(Stream<Integer> intStream, Dataframe dataframe, List<String> dataframeColumnNames, List<DataType> dataTypes) {
        boolean computeUniqueValues = dataframe.getColumnDimension() < MetadataUtils.MAX_COLUMNS_TO_ENUMERATE;
        Stream<SchemaItem> schemaItemStream;

        // four possible cases, wherein we do/do not infer types and do/do not enumerate unique values
        if (computeUniqueValues && dataTypes == null) {
            schemaItemStream = intStream.map(i -> extractRowSchemaUniquesNoDatatype(dataframe, i, dataframeColumnNames.get(i)));
        } else if (computeUniqueValues) {
            schemaItemStream = intStream.map(i -> extractRowSchemaUniquesWithDatatype(dataframe, i, dataTypes.get(i), dataframeColumnNames.get(i)));
        } else if (dataTypes == null) {
            schemaItemStream = intStream.map(i -> extractRowSchemaNoUniquesNoDatatype(dataframe, i, dataframeColumnNames.get(i)));
        } else {
            schemaItemStream = intStream.map(i -> extractRowSchemaNoUniquesWithDatatype(i, dataTypes.get(i), dataframeColumnNames.get(i)));
        }
        return Schema.from(schemaItemStream.collect(Collectors.toMap(SchemaItem::getName, Function.identity())));
    }

    public static Schema getInputSchema(Dataframe dataframe) {
        return getGenericSchema(dataframe.getInputsIndices().stream(), dataframe, dataframe.getColumnNames(), null);

    }

    public static Schema getOutputSchema(Dataframe dataframe) {
        return getGenericSchema(dataframe.getOutputsIndices().stream(), dataframe, dataframe.getColumnNames(), null);
    }

    public static Schema getInputSchema(Dataframe dataframe, List<DataType> dataTypes) {
        return getGenericSchema(dataframe.getInputsIndices().stream(), dataframe, dataframe.getColumnNames(), dataTypes);

    }

    public static Schema getOutputSchema(Dataframe dataframe, List<DataType> dataTypes) {
        return getGenericSchema(dataframe.getOutputsIndices().stream(), dataframe, dataframe.getColumnNames(), dataTypes);
    }
}
