package org.kie.trustyai.external.interfaces;

import java.util.HashMap;
import java.util.Map;

import jep.JepConfig;

public abstract class ExternalPythonClass {
    private final JepConfig config;
    private final Map<String, Object> constructionArgs = new HashMap<>();

    protected ExternalPythonClass() {
        this.config = new JepConfig();
        config.addSharedModules("numpy", "pandas", "sklearn", "scipy", "grpc");
        config.addIncludePaths("src/main/resources/python/trustyaiexternal");
    }

    public JepConfig getConfig() {
        return config;
    }

    public abstract String getNamespace();

    public abstract String getName();

    public Map<String, Object> getConstructionArgs() {
        return constructionArgs;
    }

    public void addConstructionArg(String key, Object value) {
        this.constructionArgs.put(key, value);
    }

}
