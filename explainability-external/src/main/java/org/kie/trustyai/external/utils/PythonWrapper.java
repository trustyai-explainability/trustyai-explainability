package org.kie.trustyai.external.utils;

import jep.JepConfig;
import jep.SubInterpreter;

public enum PythonWrapper {
    INSTANCE;

    private final JepConfig config;

    PythonWrapper() {
        config = new JepConfig();
        config.addSharedModules("numpy", "pandas", "sklearn", "scipy", "grpc");
        config.addIncludePaths("src/main/resources/python/trustyaiexternal");
    }

    public SubInterpreter getSubInterpreter() {
        return config.createSubInterpreter();
    }
}