package org.kie.trustyai.statistics.distributions.gaussian;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.kie.trustyai.statistics.DistributionParameters;

/**
 * Class to store the parameters of a multivariate Gaussian distribution
 */
public class MultivariateGaussianParameters implements DistributionParameters {

    public RealVector getMean() {
        return mean;
    }

    public RealMatrix getCovariance() {
        return covariance;
    }

    private final RealVector mean;
    private final RealMatrix covariance;

    private MultivariateGaussianParameters(RealVector mean, RealMatrix covariance) {
        this.mean = mean;
        this.covariance = covariance;
    }

    /**
     * Create the parameters of a multivariate Gaussian distribution
     * @param mean The mean as a {@link RealVector}
     * @param covariance The covariance as a {@link RealMatrix}
     * @return
     */
    public static MultivariateGaussianParameters create(RealVector mean, RealMatrix covariance) {
        return new MultivariateGaussianParameters(mean, covariance);
    }
}
