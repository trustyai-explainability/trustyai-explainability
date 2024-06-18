package org.kie.trustyai.explainability.local.shap.background;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.correlation.Covariance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.statistics.MultivariateOnlineEstimator;
import org.kie.trustyai.statistics.distributions.gaussian.MultivariateGaussianParameters;
import org.kie.trustyai.statistics.estimators.WelfordOnlineEstimator;

import static org.junit.jupiter.api.Assertions.*;

class StreamingGeneratorTest {

    static Stream<Arguments> replacementType() {
        return Stream.of(
                Arguments.of(StreamingGenerator.ReplacementType.RANDOM),
                Arguments.of(StreamingGenerator.ReplacementType.FIFO));
    }

    @DisplayName("Test streaming generator returns correct values")
    @ParameterizedTest
    @MethodSource("replacementType")
    void testGenerate(StreamingGenerator.ReplacementType type) {
        final int queueSize = 500;
        final int diversitySize = 50;
        final int dimension = 4;
        final int nObs = 1000;

        final MultivariateOnlineEstimator<MultivariateGaussianParameters> estimator = new WelfordOnlineEstimator(
                dimension);

        final RealVector mean = new ArrayRealVector(new double[] { 1.0, 123.0, 90.0, 90000.0 });
        final RealMatrix covariance = MatrixUtils.createRealMatrix(new double[][] {
                { 3.0, 0.0, 0.0, 0.0 },
                { 0.0, 52.0, 0.0, 0.0 },
                { 0.0, 0.0, 13.9, 0.0 },
                { 0.0, 0.0, 0.0, 30.0 }
        });

        final MultivariateNormalDistribution dist = new MultivariateNormalDistribution(mean.toArray(),
                covariance.getData());

        final double[][] truth = dist.sample(nObs);

        final StreamingGenerator generator = new StreamingGenerator(dimension, queueSize, diversitySize, estimator, type, null);

        final List<PredictionInput> initialBackground = generator.generate(queueSize + diversitySize);

        final RealMatrix bgMatrix = MatrixUtils.createRealMatrix(queueSize + diversitySize, dimension);

        for (int i = 0; i < queueSize + diversitySize; i++) {
            final double[] values = initialBackground.get(i).getFeatures().stream().mapToDouble(f -> f.getValue().asNumber())
                    .toArray();
            bgMatrix.setRow(i, values);
        }

        final Covariance initialCov = new Covariance(bgMatrix.getData());
        final RealMatrix initialCalculatedCovariance = initialCov.getCovarianceMatrix();

        for (int i = 0; i < dimension; i++) {
            final double calculatedMean = StatUtils.mean(bgMatrix.getColumn(i));
            assertEquals(0.0, calculatedMean, 2.0, "Initial mean [" + i + "] value is wrong");
            assertEquals(WelfordOnlineEstimator.DEFAULT_VARIANCE, initialCalculatedCovariance.getEntry(i, i),
                    WelfordOnlineEstimator.DEFAULT_VARIANCE / 3.0, "Initial variance [" + i + "] value is wrong");
        }

        for (int i = 0; i < nObs; i++) {
            generator.update(new ArrayRealVector(truth[i]));
        }

        final List<PredictionInput> finalBackground = generator.generate(queueSize + diversitySize);

        for (int i = 0; i < queueSize + diversitySize; i++) {
            final double[] values = finalBackground.get(i).getFeatures().stream().mapToDouble(f -> f.getValue().asNumber())
                    .toArray();
            bgMatrix.setRow(i, values);
        }
        final Covariance finalCov = new Covariance(bgMatrix.getData());
        final RealMatrix finalcalculatedCovariance = finalCov.getCovarianceMatrix();

        for (int i = 0; i < dimension; i++) {
            final double calculatedMean = StatUtils.mean(bgMatrix.getColumn(i));
            assertEquals(mean.getEntry(i), calculatedMean, mean.getEntry(i) / 3.0, "Final mean [" + i + "] value is wrong");
            final double trueVariance = covariance.getEntry(i, i);
            assertEquals(trueVariance, finalcalculatedCovariance.getEntry(i, i), trueVariance / 3.0, "Final variance " + i + " wrong");
        }

    }

    @DisplayName("Test streaming generator returns correct dimensions")
    @ParameterizedTest
    @MethodSource("replacementType")
    void testDimensions(StreamingGenerator.ReplacementType type) {
        final int queueSize = 200;
        final int diversitySize = 50;
        final int dimension = 4;
        final int nObs = 1000;

        final MultivariateOnlineEstimator<MultivariateGaussianParameters> estimator = new WelfordOnlineEstimator(
                dimension);

        final RealVector mean = new ArrayRealVector(new double[] { 1.0, 123.0, 90.0, 90000.0 });
        final RealMatrix covariance = MatrixUtils.createRealMatrix(new double[][] {
                { 10.0, 0.0, 0.0, 0.0 },
                { 0.0, 52.0, 0.0, 0.0 },
                { 0.0, 0.0, 13.9, 0.0 },
                { 0.0, 0.0, 0.0, 30.0 }
        });

        final MultivariateNormalDistribution dist = new MultivariateNormalDistribution(mean.toArray(),
                covariance.getData());

        final double[][] truth = dist.sample(nObs);

        final StreamingGenerator generator = new StreamingGenerator(dimension, queueSize, diversitySize, estimator, type, null);

        final List<PredictionInput> initialBackground = generator.generate(250);

        assertEquals(queueSize + diversitySize, initialBackground.size());

        final List<PredictionInput> initialBackgroundB = generator.generate(450);

        assertEquals(450, initialBackgroundB.size());

        for (int i = 0; i < queueSize - 100; i++) {
            generator.update(new ArrayRealVector(truth[i]));
        }

        final List<PredictionInput> finalBackground = generator.generate(250);

        for (int i = 0; i < queueSize; i++) {
            if (i < queueSize - 100) {
                assertArrayEquals(truth[i], finalBackground.get(i).getFeatures().stream().mapToDouble(f -> f.getValue().asNumber()).toArray());
            } else {
                assertFalse(Arrays.equals(truth[i], finalBackground.get(i).getFeatures().stream().mapToDouble(f -> f.getValue().asNumber()).toArray()));
            }

        }

        for (int i = queueSize - 100; i < queueSize; i++) {
            generator.update(new ArrayRealVector(truth[i]));
        }

        final List<PredictionInput> finalBackgroundC = generator.generate(250);

        for (int i = 0; i < queueSize; i++) {
            assertArrayEquals(truth[i], finalBackgroundC.get(i).getFeatures().stream().mapToDouble(f -> f.getValue().asNumber()).toArray());
        }

        final List<PredictionInput> finalBackgroundD = generator.generate(1000);
        assertEquals(1000, finalBackgroundD.size());
    }
}
