package org.kie.trustyai.statistics;

import org.apache.commons.math3.linear.RealVector;

public interface MultivariateOnlineEstimator<T extends DistributionParameters> {
    void update(RealVector data);
    T getParameters();
}
