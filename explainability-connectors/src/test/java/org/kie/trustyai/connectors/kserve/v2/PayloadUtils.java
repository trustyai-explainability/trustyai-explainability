package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.connectors.kserve.v2.grpc.KServeTarget;
import org.kie.trustyai.explainability.model.*;

public class PayloadUtils {
    private static final Random random = new Random();

    public static PredictionProvider getDummy2Provider() {
        final KServeTarget target = KServeTarget.create("0.0.0.0:8081", "dummy2", "v0.0.1", CodecParameter.PANDAS);
        return KServeV2GRPCPredictionProvider.forTarget(target);
    }

    public static PredictionProvider getDummy3Provider() {
        final KServeTarget target = KServeTarget.create("0.0.0.0:8081", "dummy3", "v0.0.1", CodecParameter.PANDAS);
        return KServeV2GRPCPredictionProvider.forTarget(target);
    }

    public static PredictionProvider getDummy4Provider() {
        final KServeTarget target = KServeTarget.create("0.0.0.0:8081", "dummy4", "v0.0.1", CodecParameter.PANDAS);
        return KServeV2GRPCPredictionProvider.forTarget(target);
    }

    public static Prediction createDummy1PredictionMixedTypes() {
        final PredictionInput predictionInput = new PredictionInput(List.of(
                FeatureFactory.newNumericalFeature("input-float", 40.83),
                FeatureFactory.newNumericalFeature("input-int", 3),
                FeatureFactory.newCategoricalFeature("input-string", "foo")));
        final PredictionOutput predictionOutput = new PredictionOutput(List.of(
                new Output("output-int", Type.NUMBER, new Value(1), 1.0),
                new Output("output-string", Type.CATEGORICAL, new Value("bar"), 1.0)));
        return new SimplePrediction(predictionInput, predictionOutput);
    }

    public static Prediction createDummy2PredictionMixedTypes() {
        final PredictionInput predictionInput = new PredictionInput(List.of(
                FeatureFactory.newNumericalFeature("input-float", 40.83),
                FeatureFactory.newNumericalFeature("input-int", 3),
                FeatureFactory.newCategoricalFeature("input-string", "foo")));
        final PredictionOutput predictionOutput = new PredictionOutput(List.of(
                new Output("output-int", Type.NUMBER, new Value(1), 1.0),
                new Output("output-string", Type.CATEGORICAL, new Value("bar"), 1.0)));
        return new SimplePrediction(predictionInput, predictionOutput);
    }

    public static Prediction createDummy3SinglePrediction() {
        final PredictionInput predictionInput = new PredictionInput(List.of(
                FeatureFactory.newNumericalFeature("input-float-1", random.nextDouble() * 100.0),
                FeatureFactory.newNumericalFeature("input-int", random.nextInt(10)),
                FeatureFactory.newNumericalFeature("input-float-2", random.nextDouble() * 100.0)));
        final PredictionOutput predictionOutput = new PredictionOutput(List.of(
                new Output("output-int-1", Type.NUMBER, new Value(random.nextInt(4)), 1.0),
                new Output("output-int-2", Type.NUMBER, new Value(random.nextInt(4) + 4), 1.0)));
        return new SimplePrediction(predictionInput, predictionOutput);
    }

    public static List<Prediction> createDummy3BatchPrediction(int n) {
        return IntStream.range(0, n).mapToObj(i -> createDummy3SinglePrediction()).collect(Collectors.toList());
    }

    public static Prediction createDummy1PredictionAllNumeric() {
        final PredictionInput predictionInput = new PredictionInput(List.of(
                FeatureFactory.newNumericalFeature("input-float-1", 40.83),
                FeatureFactory.newNumericalFeature("input-float-2", 30.1),
                FeatureFactory.newNumericalFeature("input-float-3", 17.2)));
        final PredictionOutput predictionOutput = new PredictionOutput(List.of(
                new Output("output-int", Type.NUMBER, new Value(1), 1.0),
                new Output("output-string", Type.CATEGORICAL, new Value("bar"), 1.0)));
        return new SimplePrediction(predictionInput, predictionOutput);
    }

}
