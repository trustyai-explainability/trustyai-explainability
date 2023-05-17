package org.kie.trustyai.external.interfaces;

import java.util.Map;

import jep.JepConfig;
import jep.SubInterpreter;

public abstract class ExternalPythonMetric {

    private final JepConfig config;

    protected ExternalPythonMetric() {
        this.config = new JepConfig();
        config.addSharedModules("numpy", "pandas", "sklearn", "scipy", "grpc");
        config.addIncludePaths("src/main/resources/python/trustyaiexternal");
    }

    public JepConfig getConfig() {
        return config;
    }

    public abstract String getNamespace();

    public abstract String getName();

    public abstract Map<String, Object> getConfiguration();

    public Object invoke(Map<String, Object> arguments, SubInterpreter interpreter) {

        interpreter.exec("from " + getNamespace() + " import " + getName());
        final Map<String, Object> configuration = getConfiguration();
        if (configuration.isEmpty()) {
            interpreter.exec("_metric = " + getName() + "()");
        } else {
            final Object instance = interpreter.invoke(getName(), configuration);
            interpreter.set("_metric", instance);
        }

        return interpreter.invoke("_metric.calculate", arguments).toString();
    }

}
