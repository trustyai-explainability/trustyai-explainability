package org.kie.trustyai.statistics.estimators;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.kie.trustyai.statistics.MultivariateOnlineEstimator;
import org.kie.trustyai.statistics.distributions.gaussian.MultivariateGaussianParameters;

/**
 * Welford's online algorithm for computing the mean and covariance.
 * See <a href="https://en.wikipedia.org/wiki/Algorithms_for_calculating_variance#Welford's_online_algorithm">Welford's online algorithm</a>
 * and Welford, B. P. (1962). "Note on a method for calculating corrected sums of squares and products". Technometrics. 4 (3): 419–420. doi:10.2307/1266577
 */
public class WelfordOnlineEstimator implements MultivariateOnlineEstimator<MultivariateGaussianParameters> {

    private int counter = 0;
    private RealVector mean;
    private RealMatrix covariance;
    public static double DEFAULT_VARIANCE = 100.0; // Default variance element for the initial estimation
    private final RealMatrix DEFAULT_COVARIANCE; // Default element for the initial estimation

    /**
     * Instantiate a Welford online estimator.
     * 
     * @param dimension The observation's dimension as a {@code int}
     */
    public WelfordOnlineEstimator(int dimension) {
        mean = new ArrayRealVector(dimension);
        covariance = new Array2DRowRealMatrix(dimension, dimension);
        DEFAULT_COVARIANCE = MatrixUtils.createRealIdentityMatrix(dimension).scalarMultiply(DEFAULT_VARIANCE);
    }

    /**
     * Update the mean and covariance with an observation.
     *
     * @param observation An observation as a {@link RealVector}
     */
    @Override
    public synchronized void update(RealVector observation) {
        // delta = x - mean
        final RealVector delta = observation.subtract(mean);
        // mean = mean + delta / (1 + n)
        mean = mean.add(delta.mapDivide(1.0 + counter));

        if (counter > 0) {
            // covariance = (covariance * n) / (1 + n) + n / (1 + n)^2 * (delta * delta')
            final RealMatrix C1 = covariance.scalarMultiply((double) counter / (1.0 + counter));
            final RealMatrix delta2 = delta.outerProduct(delta);
            covariance = C1.add(delta2.scalarMultiply((double) counter / ((1.0 + counter) * (1.0 + counter))));
        }
        // n = n + 1
        counter += 1;

    }

    /**
     * Return the estimated parameters of the multivariate Gaussian distribution
     * 
     * @return Estimated parameters as {@link MultivariateGaussianParameters}
     */
    @Override
    public MultivariateGaussianParameters getParameters() {
        if (counter < 1) {
            return MultivariateGaussianParameters.create(mean, DEFAULT_COVARIANCE);
        } else {
            return MultivariateGaussianParameters.create(mean, covariance.scalarMultiply(counter / (counter - 1.0)));
        }
    }
}
