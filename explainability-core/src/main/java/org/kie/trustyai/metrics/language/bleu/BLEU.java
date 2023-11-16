package org.kie.trustyai.metrics.language.bleu;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.kie.trustyai.metrics.language.AbstractNLPPerformanceMetric;
import org.kie.trustyai.metrics.language.bleu.smoothing.SmoothingFunction;
import org.kie.trustyai.metrics.language.bleu.smoothing.SmoothingOriginal;
import org.kie.trustyai.metrics.language.utils.NGramUtils;
import org.kie.trustyai.metrics.language.utils.NLPUtils;

public class BLEU extends AbstractNLPPerformanceMetric {

    private static final int DEFAULT_MAX_NGRAMS = 4; // BLEU-4

    private final SmoothingFunction smoothingFunction;

    public BLEU() {
        this(new SmoothingOriginal());
    }

    public BLEU(SmoothingFunction smoothingFunction) {
        this.smoothingFunction = smoothingFunction;
    }

    public static double[] createUniformWeights(int maxNgram) {
        // Create uniform weights
        final double uniformWeight = 1.0 / maxNgram;
        final double[] weights = new double[maxNgram];
        Arrays.fill(weights, uniformWeight);
        return weights;
    }

    /**
     * Calculate brevity penalty.
     *
     * @param closestReferenceLength The length of the closest reference.
     * @param hypothesisLength The length of the hypothesis.
     * @return BLEU's brevity penalty.
     */
    public static double brevityPenalty(int closestReferenceLength, int hypothesisLength) {
        if (hypothesisLength > closestReferenceLength) {
            return 1.0;
        } else if (hypothesisLength == 0) {
            return 0.0;
        } else {
            return Math.exp(1 - (double) closestReferenceLength / hypothesisLength);
        }
    }

    /**
     * Calculates the BLEU score for a list of references and a hypothesis using uniform weights.
     * Default is BLEU-4 (4 maximum n-gram order, uniform weights).
     *
     * @param references A list of reference strings.
     * @param hypothesis The hypothesis string.
     * @return The BLEU score.
     */
    public double calculateSentence(List<String> references, String hypothesis) {
        return calculateSentence(references, hypothesis, DEFAULT_MAX_NGRAMS);
    }

    /**
     * Calculates the BLEU score for a list of references and a hypothesis using uniform weights.
     *
     * @param references A list of reference strings.
     * @param hypothesis The hypothesis string.
     * @param maxNgram The maximum n-gram order.
     * @return The BLEU score.
     */
    public double calculateSentence(List<String> references, String hypothesis, int maxNgram) {
        return calculateSentence(references, hypothesis, maxNgram, createUniformWeights(maxNgram));
    }

    /**
     * Calculates the BLEU score for a list of references and a hypothesis.
     *
     * @param references A list of reference strings.
     * @param hypothesis The hypothesis string.
     * @param maxNgram The maximum n-gram order.
     * @param weights The weights for each n-gram order.
     * @return The BLEU score.
     */
    public double calculateSentence(List<String> references, String hypothesis, int maxNgram, double[] weights) {
        if (weights.length != maxNgram) {
            throw new IllegalArgumentException("Weights array must be the same length as maxNgram");
        }

        final String[] hypothesisTokens = this.getTokenizer().tokenize(hypothesis);
        final double[] rawPrecisionScores = new double[maxNgram];
        final int hypothesisLength = hypothesisTokens.length;
        final int closestReferenceLength = NLPUtils.closestReferenceLength(List.of(references), hypothesisLength);

        for (int n = 1; n <= maxNgram; n++) {
            int totalMatchCount = 0;
            final int possibleNgrams = Math.max(hypothesisLength - n + 1, 0);

            final Map<String, Integer> hypothesisNgramCount = NGramUtils.countNgrams(NGramUtils.generateNgrams(hypothesisTokens, n));
            final Map<String, Integer> maxReferenceNgramCount = new HashMap<>();

            for (String reference : references) {
                final String[] referenceTokens = this.getTokenizer().tokenize(reference);
                final Map<String, Integer> referenceNgramCount = NGramUtils.countNgrams(NGramUtils.generateNgrams(referenceTokens, n));

                for (String ngram : hypothesisNgramCount.keySet()) {
                    final int refCount = referenceNgramCount.getOrDefault(ngram, 0);
                    maxReferenceNgramCount.put(ngram, Math.max(maxReferenceNgramCount.getOrDefault(ngram, 0), refCount));
                }
            }

            for (Map.Entry<String, Integer> entry : hypothesisNgramCount.entrySet()) {
                final String ngram = entry.getKey();
                final int count = entry.getValue();
                totalMatchCount += Math.min(count, maxReferenceNgramCount.getOrDefault(ngram, 0));
            }

            rawPrecisionScores[n - 1] = possibleNgrams > 0 ? (double) totalMatchCount / possibleNgrams : 0;
        }

        final double[] smoothedPrecisionScores = this.smoothingFunction.apply(rawPrecisionScores);
        double score = 1.0;

        for (int i = 0; i < maxNgram; i++) {
            if (smoothedPrecisionScores[i] > 0) {
                score *= Math.pow(smoothedPrecisionScores[i], weights[i]);
            } else if (weights[i] > 0) {
                // If the precision score is 0 and the weight is not zero, then the BLEU score should be 0.
                return 0;
            }
        }

        score = Math.pow(score, 1.0 / Arrays.stream(weights).sum());

        // Apply brevity penalty
        score *= brevityPenalty(closestReferenceLength, hypothesisLength);

        return score;
    }

    public double modifiedPrecision(List<String> references, String hypothesis, int n) {
        // Tokenize the hypothesis
        final String[] hypothesisTokens = this.getTokenizer().tokenize(hypothesis);
        final List<String> hypothesisNgrams = NGramUtils.generateNgrams(hypothesisTokens, n);
        final Map<String, Integer> hypothesisNgramCounts = NGramUtils.countNgrams(hypothesisNgrams);

        // Store the maximum n-gram counts for all references
        final Map<String, Integer> maxReferenceNgramCounts = references.stream()
                .map(ref -> this.getTokenizer().tokenize(ref)) // Tokenize each reference
                .flatMap(refTokens -> NGramUtils.generateNgrams(refTokens, n).stream()) // Generate n-grams
                .collect(Collectors.toMap(
                        ngram -> ngram,
                        ngram -> 1,
                        Integer::max));

        // Clip the hypothesis n-gram counts using the max counts from references
        int clippedCount = 0;
        for (Map.Entry<String, Integer> entry : hypothesisNgramCounts.entrySet()) {
            final String ngram = entry.getKey();
            final int hypCount = entry.getValue();
            final int maxCount = maxReferenceNgramCounts.getOrDefault(ngram, 0);
            clippedCount += Math.min(hypCount, maxCount);
        }

        // Calculate total n-grams in the hypothesis
        final int totalNgrams = hypothesisNgrams.size();

        // Return the modified precision score
        return totalNgrams > 0 ? (double) clippedCount / totalNgrams : 0.0;
    }

    /**
     * Calculate n-gram precision given a list of reference sentences and a hypothesis
     * 
     * @param refTokenLists
     * @param hypTokens
     * @param n
     * @return
     */
    private int[] calculateNGramPrecision(List<List<String>> refTokenLists, List<String> hypTokens, int n) {
        final Map<String, Integer> maxRefNgramCounts = new HashMap<>();
        int clippedCount = 0;
        final int totalNgrams = Math.max(hypTokens.size() - n + 1, 0);

        // Create the max reference n-gram counts
        for (List<String> refTokens : refTokenLists) {
            final Map<String, Integer> refNgramCounts = NGramUtils.countNgrams(refTokens, n);
            for (Map.Entry<String, Integer> entry : refNgramCounts.entrySet()) {
                String ngram = entry.getKey();
                int count = entry.getValue();
                maxRefNgramCounts.put(ngram, Math.max(maxRefNgramCounts.getOrDefault(ngram, 0), count));
            }
        }

        // Count the matches, clipped by the max reference counts
        final Map<String, Integer> hypNgramCounts = NGramUtils.countNgrams(hypTokens, n);
        for (Map.Entry<String, Integer> entry : hypNgramCounts.entrySet()) {
            final String ngram = entry.getKey();
            final int hypCount = entry.getValue();
            final int refCount = maxRefNgramCounts.getOrDefault(ngram, 0);
            clippedCount += Math.min(hypCount, refCount);
        }

        return new int[] { clippedCount, totalNgrams };
    }

    /**
     * Method to calculate the BLEU score for the entire corpus
     *
     * @param references Corpus of reference sentences
     * @param hypotheses Hypoteses sentences
     * @param weights weights for n-grams
     * @return Corpus BLEU score
     */
    public double calculateCorpus(List<List<String>> references, List<String> hypotheses, double[] weights) {
        if (references.size() != hypotheses.size()) {
            throw new IllegalArgumentException("The number of hypotheses and their reference sets should be the same");
        }

        // Initialize variables for BLEU score calculation
        final Map<Integer, Integer> pNumerators = new HashMap<>();
        final Map<Integer, Integer> pDenominators = new HashMap<>();
        int hypLengthSum = 0;
        int refLengthSum = 0;

        // Default weights for n-gram precision
        if (weights == null || weights.length == 0) {
            weights = createUniformWeights(DEFAULT_MAX_NGRAMS);
        }

        // Process each hypothesis and its set of references
        for (int i = 0; i < hypotheses.size(); i++) {
            final String hypothesis = hypotheses.get(i);
            final List<String> hypTokens = List.of(this.getTokenizer().tokenize(hypothesis));
            hypLengthSum += hypTokens.size();

            final List<List<String>> refTokenLists = references.get(i).stream()
                    .map(reference -> List.of(this.getTokenizer().tokenize(reference)))
                    .collect(Collectors.toList());

            // Find the closest reference length for the current hypothesis
            final int closestRefLen = NLPUtils.closestReferenceLength(refTokenLists, hypTokens.size());
            refLengthSum += closestRefLen;

            // Calculate precision for each n-gram order
            for (int n = 1; n <= weights.length; n++) {
                final int[] ngramResults = calculateNGramPrecision(refTokenLists, hypTokens, n);
                pNumerators.put(n, pNumerators.getOrDefault(n, 0) + ngramResults[0]);
                pDenominators.put(n, pDenominators.getOrDefault(n, 0) + ngramResults[1]);
            }
        }

        // Calculate corpus-level brevity penalty
        final double bp = brevityPenalty(refLengthSum, hypLengthSum);

        final double[] rawPrecisions = new double[weights.length];
        for (int n = 1; n <= weights.length; n++) {
            rawPrecisions[n - 1] = pNumerators.getOrDefault(n, 0) / (double) pDenominators.getOrDefault(n, 1);
        }

        // Apply smoothing
        final double[] smoothedPrecisions = smoothingFunction.apply(rawPrecisions);

        // Calculate score using weighted geometric mean of the smoothed precisions
        final double[] fWeights = weights;
        final double bleuScore = IntStream.range(0, weights.length)
                .mapToDouble(n -> {
                    double smoothedPrecision = smoothedPrecisions[n];
                    return fWeights[n] * (smoothedPrecision > 0 ? Math.log(smoothedPrecision) : 0);
                }).sum();

        return bp * Math.exp(bleuScore);

    }

}
