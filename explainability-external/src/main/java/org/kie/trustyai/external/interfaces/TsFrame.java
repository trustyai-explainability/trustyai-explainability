package org.kie.trustyai.external.interfaces;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.external.utils.Converter;

import jep.SubInterpreter;
import jep.python.PyObject;

public class TsFrame {

    public final static String DEFAULT_FORMAT = "%Y-%m-%d";

    private final Dataframe dataframe;
    private final String timestampColumn;
    private final String format;

    public TsFrame(Dataframe dataframe, String timestampColumn, String format) {
        this.dataframe = dataframe;
        this.timestampColumn = timestampColumn;
        this.format = format;
    }

    public TsFrame(Dataframe dataframe, String timestampColumn) {
        this(dataframe, timestampColumn, DEFAULT_FORMAT);
    }

    public PyObject getTsFrame(final SubInterpreter interpreter) {
        return Converter.dataframeToTsframe(dataframe, this.timestampColumn, this.format, interpreter);
    }
}
