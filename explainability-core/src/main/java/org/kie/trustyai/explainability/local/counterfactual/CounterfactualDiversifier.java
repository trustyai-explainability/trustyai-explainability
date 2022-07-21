/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.trustyai.explainability.local.counterfactual;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.explainability.model.Dataset;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.Output;
import org.kie.trustyai.explainability.model.Prediction;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.kie.trustyai.explainability.model.SimplePrediction;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

/**
 * Counterfactual diversity generator.
 *
 * Adapted from MACE's diversity generator (<a href="https://arxiv.org/abs/1905.11190">Model-Agnostic Counterfactual Explanations for Consequential Decisions</a>)
 */
public class CounterfactualDiversifier {

    private final PredictionProvider model;
    private final List<Feature> original;
    private final CounterfactualResult result;

    private final int nSamples;
    private final Random rng;

    private final int nFeatures;

    private final List<Output> goal;
    private final BiFunction<List<Feature>, Prediction, Double> distanceCriteria;

    protected static final BiFunction<List<Feature>, Prediction, Double> DEFAULT_DISTANCE_CRITERIA = (reference, candidate) -> {
        int sparsity = 0;
        double distance = 0.0;
        final int N = reference.size();
        for (int f = 0; f < N; f++) {
            final Feature referenceFeature = reference.get(f);
            final Value referenceValue = referenceFeature.getValue();
            final Feature candidateFeature = candidate.getInput().getFeatures().get(f);
            final Value candidateValue = candidateFeature.getValue();
            if (!referenceValue.equals(candidateValue)) {
                if (referenceFeature.getType() == Type.NUMBER) {
                    distance += Math.abs(referenceValue.asNumber() - candidateValue.asNumber());
                } else {
                    distance += (referenceValue.equals(candidateValue) ? 0.0 : 1.0);
                }
                sparsity += 1;
            }
        }
        final double meanScore = candidate.getOutput().getOutputs().stream().mapToDouble(Output::getScore).average().getAsDouble();
        final double result = ((distance - sparsity) / N) - meanScore;

        return result;
    };
    private static final Function<Output, Object> getValueObject = item -> item.getValue().getUnderlyingObject();

    private class FeatureDistance {
        public double getDistance() {
            return distance;
        }

        public Prediction getPrediction() {
            return prediction;
        }

        private final double distance;
        private final Prediction prediction;

        FeatureDistance(Prediction prediction, double distance) {
            this.prediction = prediction;
            this.distance = distance;
        }
    }

    /**
     * Instantiate a diverse counterfactual generator.
     *
     * @param model The {@link PredictionProvider} required for predicting new counterfactuals
     * @param original The original reference list of {@link Feature}
     * @param result The original {@link CounterfactualResult}
     * @param nSamples The number of samples to create
     * @param goal The counterfactual's goal as a {@link List<Output>}
     */
    public CounterfactualDiversifier(PredictionProvider model,
            List<Feature> original, CounterfactualResult result, int nSamples, List<Output> goal,
            BiFunction<List<Feature>, Prediction, Double> distanceCriteria) {
        this.model = model;
        this.original = original;
        this.nFeatures = original.size();
        this.result = result;
        this.nSamples = nSamples;
        this.goal = goal;
        this.distanceCriteria = distanceCriteria;
        this.rng = new Random();
    }

    /**
     * Generate dataset with diverse counterfactuals.
     *
     * @param maximum Maximum number of diverse counterfactuals to generate
     * @return A {@link Dataset} with the diverse counterfactuals.
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Dataset diversify(int maximum) throws ExecutionException, InterruptedException {
        final Dataset dataset = this.generateAlternatives();
        final List<Prediction> data = dataset.getData();
        final List<FeatureDistance> distances = data
                .stream()
                .map(d -> new FeatureDistance(d, this.distanceCriteria.apply(original, d)))
                .sorted(Comparator.comparing(FeatureDistance::getDistance)).collect(Collectors.toList());
        final List<Prediction> predictions = distances.stream().limit(maximum).map(FeatureDistance::getPrediction).collect(Collectors.toList());
        return new Dataset(predictions);
    }

    public static boolean goalMatches(List<Output> output, List<Output> goal) {
        final List<Object> outputValues = output.stream().map(getValueObject).collect(Collectors.toList());
        final List<Object> goalValues = goal.stream().map(getValueObject).collect(Collectors.toList());

        return outputValues.equals(goalValues);
    }

    private double[] generateProbabilities() {
        final double step = 0.5 / nFeatures;
        return IntStream.range(0, nFeatures)
                .mapToDouble(x -> (x + this.rng.nextDouble()) * step + 0.5)
                .toArray();
    }

    /**
     * Calculate permutations between counterfactuals and reference point
     * 
     * @return A dataset of alternative counterfactuals
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public Dataset generateAlternatives() throws ExecutionException, InterruptedException {
        final Set<List<Feature>> uniqueFeatures = new HashSet<>();
        final List<Prediction> predictions = new ArrayList<>();
        final double[] probabilities = generateProbabilities();
        for (int i = 0; i < this.nSamples; i++) {
            for (double probability : probabilities) {
                final double[] featureProbabilities = new Random().doubles(this.nFeatures).toArray();
                final List<Feature> alternativeFeatures = new ArrayList<>();
                for (int f = 0; f < nFeatures; f++) {
                    if (featureProbabilities[f] < probability) {
                        alternativeFeatures.add(result.getEntities().get(f).asFeature());
                    } else {
                        alternativeFeatures.add(original.get(f));
                    }
                }

                if (!uniqueFeatures.contains(alternativeFeatures)) {
                    uniqueFeatures.add(alternativeFeatures);
                    final List<PredictionInput> inputs = List.of(new PredictionInput(alternativeFeatures));
                    final List<PredictionOutput> outputs = model.predictAsync(inputs).get();
                    final Prediction prediction = new SimplePrediction(inputs.get(0), outputs.get(0));
                    if (goalMatches(prediction.getOutput().getOutputs(), this.goal)) {
                        predictions.add(prediction);
                    }
                }
            }
        }

        return new Dataset(predictions);
    }

    public static CounterfactualDiversifier.Builder builder(PredictionProvider model,
            List<Feature> original, CounterfactualResult result, List<Output> goal) {
        return new CounterfactualDiversifier.Builder(model, original, result, goal);
    }

    public static class Builder {
        public static final int DEFAULT_NSAMPLES = 100;
        private int nSamples = DEFAULT_NSAMPLES;
        private final PredictionProvider model;
        private final List<Feature> original;
        private final CounterfactualResult result;
        private final List<Output> goal;

        private BiFunction<List<Feature>, Prediction, Double> distanceCriteria = DEFAULT_DISTANCE_CRITERIA;

        private Builder(PredictionProvider model,
                List<Feature> original, CounterfactualResult result, List<Output> goal) {
            this.model = model;
            this.original = original;
            this.result = result;
            this.goal = goal;
        }

        public CounterfactualDiversifier build() {
            return new CounterfactualDiversifier(model, original, result, nSamples, goal, distanceCriteria);
        }

        public CounterfactualDiversifier.Builder withNSamples(int nSamples) {
            this.nSamples = nSamples;
            return this;
        }

        public CounterfactualDiversifier.Builder withDistanceCriteria(BiFunction<List<Feature>, Prediction, Double> criteria) {
            this.distanceCriteria = criteria;
            return this;
        }

    }

}
