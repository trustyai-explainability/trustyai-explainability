package org.kie.trustyai.external.metrics.fairness.group;

import java.util.HashMap;
import java.util.Map;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.external.interfaces.BinaryClassificationMetric;
import org.kie.trustyai.external.interfaces.ExternalPythonMetric;
import org.kie.trustyai.external.utils.Converter;

import jep.SubInterpreter;

public class AIFDisparateImpactRatio extends ExternalPythonMetric implements BinaryClassificationMetric {

    private final String NAMESPACE = "trustyaiexternal.algorithms.dir";
    private final String NAME = "DIR";

    @Override
    public double calculate(Dataframe df) {
        final SubInterpreter interpreter = this.getConfig().createSubInterpreter();
        final Object pyDF = Converter.arrayToDataframe(df, interpreter);

        final Map<String, Object> arguments = new HashMap<>();
        arguments.put("df", pyDF);

        double result = Double.parseDouble(this.invoke(arguments, interpreter).toString());
        interpreter.close();
        return result;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return null;
    }
}
