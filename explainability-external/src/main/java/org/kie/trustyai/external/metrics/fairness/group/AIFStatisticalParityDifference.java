package org.kie.trustyai.external.metrics.fairness.group;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.external.interfaces.BinaryClassificationMetric;
import org.kie.trustyai.external.interfaces.ExternalPythonMetric;
import org.kie.trustyai.external.utils.Converter;

import jep.NDArray;
import jep.SubInterpreter;

public class AIFStatisticalParityDifference extends ExternalPythonMetric implements BinaryClassificationMetric {

    private final String NAMESPACE = "trustyaiexternal.algorithms.spd";
    private final String NAME = "SPD";
    private final List<String> protectedAttributeNames;
    private final List<String> labelNames;
    private final double favorableLabel;
    private final double unfavorableLabel;
    private final List<List<Double>> privilegedProtectedAttributes;
    private final List<List<Double>> unprivilegedProtectedAttributes;
    private final List<Double> unprivilegedGroups;
    private final List<Double> privilegedGroups;

    public AIFStatisticalParityDifference(List<String> protectedAttributeNames,
            List<String> labelNames,
            double favorableLabel,
            double unfavorableLabel,
            List<List<Double>> privilegedProtectedAttributes,
            List<List<Double>> unprivilegedProtectedAttributes,
            List<Double> unprivilegedGroups,
            List<Double> privilegedGroups) {
        super();
        this.protectedAttributeNames = protectedAttributeNames;
        this.labelNames = labelNames;
        this.favorableLabel = favorableLabel;
        this.unfavorableLabel = unfavorableLabel;
        this.privilegedProtectedAttributes = privilegedProtectedAttributes;
        this.unprivilegedProtectedAttributes = unprivilegedProtectedAttributes;
        this.unprivilegedGroups = unprivilegedGroups;
        this.privilegedGroups = privilegedGroups;
    }

    @Override
    public double calculate(Dataframe df) {
        final SubInterpreter interpreter = this.getConfig().createSubInterpreter();

        final Object pyDf = Converter.arrayToDataframe(df, interpreter);

        final Map<String, Object> arguments = new HashMap<>();
        arguments.put("df", pyDf);

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
        final Map<String, Object> configuration = new HashMap<>();
        configuration.put("protected_attribute_names", protectedAttributeNames);
        configuration.put("label_names", labelNames);
        configuration.put("favorable_label", favorableLabel);
        configuration.put("unfavorable_label", unfavorableLabel);
        configuration.put("privileged_protected_attributes",
                privilegedProtectedAttributes.stream().map(attribute -> new NDArray<double[]>(attribute.stream().mapToDouble(Double::doubleValue).toArray())).collect(Collectors.toList()));
        configuration.put("unprivileged_protected_attributes",
                unprivilegedProtectedAttributes.stream().map(attribute -> new NDArray<double[]>(attribute.stream().mapToDouble(Double::doubleValue).toArray())).collect(Collectors.toList()));
        configuration.put("unprivileged_groups", unprivilegedGroups);
        configuration.put("privileged_groups", privilegedGroups);
        return configuration;
    }
}
