package org.kie.trustyai.explainability.local.shap.background;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.MultivariateNormalDistribution;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.statistics.MultivariateOnlineEstimator;
import org.kie.trustyai.statistics.distributions.gaussian.MultivariateGaussianParameters;

/**
 * Streaming background generator for SHAP.
 * <p>
 * This generator fill a queue with observation and uses an online estimator for the underlying distribution,
 * assumed Gaussian.
 * Missing observations are filled with synthetic data from the distribution and an extra set of samples
 * is produced for diversity.
 */
public class StreamingGenerator implements BackgroundGenerator {

    private final int queueSize;
    private final RealMatrix queue;
    private final int diversitySize;
    private final MultivariateOnlineEstimator<MultivariateGaussianParameters> estimator;
    private final Random random;
    private final ReplacementType replacementType;
    private final List<String> featureNames;
    private int counter = 0;

    /**
     * Creates a SHAP streaming background generator.
     * The replacement method use is random.
     *
     * @param dimensions The observation's dimension
     * @param queueSize The required number of real observations
     * @param diversitySize The required number of diversity samples
     * @param estimator The estimator to use as a {@link MultivariateOnlineEstimator}
     */
    public StreamingGenerator(int dimensions,
            int queueSize,
            int diversitySize,
            MultivariateOnlineEstimator<MultivariateGaussianParameters> estimator) {
        this(dimensions, queueSize, diversitySize, estimator, ReplacementType.RANDOM, null);
    }

    /**
     * Creates a SHAP streaming background generator.
     *
     * @param dimensions The observation's dimension
     * @param queueSize The required number of real observations
     * @param diversitySize The required number of diversity samples
     * @param estimator The estimator to use as a {@link MultivariateOnlineEstimator}
     * @param replacementType The replacement type as a {@link ReplacementType}
     * @param featureNames The list of feature names. If {@code null} a default set of names will be created.
     */
    public StreamingGenerator(int dimensions,
            int queueSize,
            int diversitySize,
            MultivariateOnlineEstimator<MultivariateGaussianParameters> estimator,
            ReplacementType replacementType,
            List<String> featureNames) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("Data dimension must be positive.");
        }
        if (queueSize <= 0) {
            throw new IllegalArgumentException("Queue size must be positive.");
        }

        this.replacementType = replacementType;
        this.queueSize = queueSize;
        this.diversitySize = diversitySize;
        this.queue = MatrixUtils.createRealMatrix(queueSize, dimensions);
        this.estimator = estimator;
        if (Objects.isNull(featureNames)) {
            this.featureNames = IntStream.range(0, dimensions).mapToObj(i -> "inputs-" + i).collect(Collectors.toUnmodifiableList());
        } else {
            this.featureNames = featureNames;
        }

        this.random = new Random();
    }

    private PredictionInput vectorToPredictionInput(RealVector vector) {
        final int size = vector.getDimension();
        final List<Feature> features = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            final Feature feature = FeatureFactory.newNumericalFeature(featureNames.get(i), vector.getEntry(i));
            features.add(feature);
        }
        return new PredictionInput(features);
    }

    public synchronized void update(RealVector data) {
        if (counter < queueSize) {
            queue.setRowVector(counter, data);
            counter++;
        } else {
            if (replacementType == ReplacementType.RANDOM) {
                final int randomIndex = random.nextInt(queueSize);
                queue.setRowVector(randomIndex, data);
            } else if (replacementType == ReplacementType.FIFO) {
                final RealMatrix queueFIFO = queue.copy();
                for (int i = 0; i < queueSize - 1; i++) {
                    queue.setRow(i, queueFIFO.getRow(i + 1));
                }
                queue.setRowVector(queueSize - 1, data);
            }
        }
        estimator.update(data);
    }

    @Override
    public List<PredictionInput> generate(int n) {
        final List<PredictionInput> inputs = new ArrayList<>();
        final MultivariateGaussianParameters parameters = estimator.getParameters();
        final MultivariateNormalDistribution dataDistribution = new MultivariateNormalDistribution(
                parameters.getMean().toArray(), parameters.getCovariance().getData());
        final int totalSize = Math.max(queueSize + diversitySize, n);
        final int startIndex;
        final int endIndex;
        if (counter < queueSize) {
            endIndex = counter;
            startIndex = counter;
        } else {
            endIndex = queueSize;
            startIndex = queueSize;
        }
        for (int i = 0; i < endIndex; i++) {
            final RealVector row = queue.getRowVector(i);
            inputs.add(vectorToPredictionInput(row));
        }
        for (int i = startIndex; i < totalSize; i++) {
            final RealVector syntheticData = new ArrayRealVector(dataDistribution.sample());
            inputs.add(vectorToPredictionInput(syntheticData));
        }

        return inputs;
    }

    /**
     * Generator's replacement type, for when the queue is full.
     * {@code RANDOM} replaces a previous observation at random.
     * {@code FIFO} replaces the oldest observation, allowing forgetting.
     */
    public enum ReplacementType {
        RANDOM,
        FIFO
    }
}
