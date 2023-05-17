package org.kie.trustyai.external.interfaces;

import org.kie.trustyai.explainability.model.Dataframe;

public interface ExternalMetric {
    public double calculate(Dataframe dataframe);
}
