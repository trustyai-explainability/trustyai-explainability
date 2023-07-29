package org.kie.trustyai.external.interfaces;

import java.util.HashMap;
import java.util.Map;

public abstract class ExternalPythonClass {

    private final Map<String, Object> constructionArgs = new HashMap<>();

    protected ExternalPythonClass() {
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
