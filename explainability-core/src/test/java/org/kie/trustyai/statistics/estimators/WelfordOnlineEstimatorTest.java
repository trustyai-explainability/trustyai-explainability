package org.kie.trustyai.statistics.estimators;

import java.util.stream.Stream;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.trustyai.statistics.distributions.gaussian.MultivariateGaussianParameters;

import static org.junit.jupiter.api.Assertions.*;
import static org.kie.trustyai.statistics.estimators.WelfordOnlineEstimator.DEFAULT_VARIANCE;

class WelfordOnlineEstimatorTest {

    static Stream<Arguments> provideParameters() {
        return Stream.of(
                Arguments.of(23.0, 7.0, 100.0, 100.0),
                Arguments.of(3, 5000, 34.1, 2.0),
                Arguments.of(5, 5, 10.0, 10.0),
                Arguments.of(51, 90000, 4.0, 15.0));
    }

    @DisplayName("Test correct mean and variance estimation")
    @ParameterizedTest
    @MethodSource("provideParameters")
    void testWelfordOnlineCorrect(double mean0, double mean1, double var0, double var1) {
        final MultivariateNormalDistribution mvn = new MultivariateNormalDistribution(
                new double[] { mean0, mean1 },
                new double[][] { { var0, 0.0 }, { 0.0, var1 } });

        final int nSamples = 1000;
        final double[][] truth = mvn.sample(nSamples);

        final WelfordOnlineEstimator s = new WelfordOnlineEstimator(2);

        for (int i = 0; i < nSamples; i++) {
            s.update(new ArrayRealVector(truth[i]));
        }

        final MultivariateGaussianParameters parameters = s.getParameters();
        assertEquals(mean0, parameters.getMean().getEntry(0), mean0 / 10.0);
        assertEquals(mean1, parameters.getMean().getEntry(1), mean1 / 10.0);
        assertEquals(var0, parameters.getCovariance().getEntry(0, 0), var0 / 10.0);
        assertEquals(var1, parameters.getCovariance().getEntry(1, 1), var1 / 10.0);
    }

    @DisplayName("Test default mean and variance")
    @Test
    void testWelforOnlineDefaultCov() {
        WelfordOnlineEstimator s = new WelfordOnlineEstimator(2);

        final MultivariateGaussianParameters parameters = s.getParameters();

        assertEquals(0.0, parameters.getMean().getEntry(0));
        assertEquals(0.0, parameters.getMean().getEntry(1));
        assertEquals(DEFAULT_VARIANCE, parameters.getCovariance().getEntry(0, 0));
        assertEquals(DEFAULT_VARIANCE, parameters.getCovariance().getEntry(1, 1));
    }

}
