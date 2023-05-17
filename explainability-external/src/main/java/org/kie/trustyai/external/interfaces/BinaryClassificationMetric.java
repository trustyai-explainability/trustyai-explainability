package org.kie.trustyai.external.interfaces;

import org.kie.trustyai.explainability.model.Dataframe;

public interface BinaryClassificationMetric {
    double calculate(Dataframe df);
}
