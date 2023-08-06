package org.kie.trustyai.external.utils;

import jep.JepConfig;
import jep.SubInterpreter;

public enum PythonRuntimeManager {
    INSTANCE;

    private final JepConfig config;

    PythonRuntimeManager() {
        config = new JepConfig();
        final String[] MODULES = new String[] { "numpy", "pandas", "sklearn", "scipy", "requests" };
        config.addSharedModules(MODULES);
    }

    /**
     * This interpreter is shared across all threads, but each thread has its own scope.
     * The interpreter must be closed before creating a new one on the same thread.
     *
     * A typical usage is:
     *
     * <pre>
     * try (SubInterpreter sub = PythonRuntimeManager.INSTANCE.getSubInterpreter()) {
     *     // Do something with the interpreter
     * }
     * // Here the interpreter was automatically closed
     * </pre>
     * 
     * @return
     */
    public SubInterpreter getSubInterpreter() {
        return config.createSubInterpreter();
    }
}