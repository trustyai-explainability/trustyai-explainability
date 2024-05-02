package org.kie.trustyai.connectors.kserve;

import java.util.List;

public abstract class AbstractKServePredictionProvider {
    protected static final String DEFAULT_TENSOR_NAME = "predict";
    protected static final KServeDatatype DEFAULT_DATATYPE = KServeDatatype.FP64;
    protected final List<String> outputNames;
    protected final String inputName;

    protected AbstractKServePredictionProvider(List<String> outputNames, String inputName) {
        this.outputNames = outputNames;
        this.inputName = inputName;
    }
}
