package org.kie.trustyai.metrics.language.nlp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.metrics.language.bleu.BLEU;
import org.kie.trustyai.metrics.language.bleu.smoothing.SmoothingFunction;
import org.kie.trustyai.metrics.language.bleu.smoothing.SmoothingFunctionEpsilon;
import org.kie.trustyai.metrics.language.utils.NLPUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validation tests based on NLTK's BLEU tests
 */
class BLEUTest {

    final static String commonHypothesis = "the cat the cat on mat";
    final static String validationHypothesisA = "It is a guide to action which ensures that the military always obeys the commands of the party";
    final static String validationHypothesisB = "It is to insure the troops forever hearing the activity guidebook that party direct";
    private static final String common = "the cat is on the mat";
    private static final String uncommon = "The candidate has no alignment to any of the references";
    private static final List<String> commonReferences = List.of(common, common);
    private static final List<String> validationReference = List.of("It is a guide to action that ensures that the military will forever heed Party commands",
            "It is the guiding principle which guarantees the military forces always being under the command of the Party",
            "It is the practical guide for the army always to heed the directions of the party");

    private static final List<String> corpusHypotheses = List.of("It is a guide to action which ensures that the military always obeys the commands of the party",
            "he read the book because he was interested in world history");

    private static final List<String> corpusReferences = List.of("It is a guide to action that ensures that the military will forever heed Party commands",
            "It is the guiding principle which guarantees the military forces always being under the command of the Party",
            "It is the practical guide for the army always to heed the directions of the party",
            "he was interested in world history because he read the book");

    private static final String hyp1 = String.join(" ", new String[] {
            "It", "is", "a", "guide", "to", "action", "which",
            "ensures", "that", "the", "military", "always",
            "obeys", "the", "commands", "of", "the", "party"
    });
    private static final String hyp2 = String.join(" ", new String[] {
            "he", "read", "the", "book", "because", "he", "was",
            "interested", "in", "world", "history"
    });

    // References for hyp1
    private static final String ref1a = String.join(" ", new String[] {
            "It", "is", "a", "guide", "to", "action", "that",
            "ensures", "that", "the", "military", "will", "forever",
            "heed", "Party", "commands"
    });
    private static final String ref1b = String.join(" ", new String[] {
            "It", "is", "the", "guiding", "principle", "which",
            "guarantees", "the", "military", "forces", "always",
            "being", "under", "the", "command", "of", "the", "Party"
    });
    private static final String ref1c = String.join(" ", new String[] {
            "It", "is", "the", "practical", "guide", "for", "the",
            "army", "always", "to", "heed", "the", "directions",
            "of", "the", "party"
    });

    // References for hyp2
    private static final String ref2a = String.join(" ", new String[] {
            "he", "was", "interested", "in", "world", "history",
            "because", "he", "read", "the", "book"
    });

    @Test
    @DisplayName("Calculate BLEU for simple inputs/hypotheses (smoothing, weights)")
    void calculateSentenceEpsilonSmoothingWeights() {
        final SmoothingFunction epsilon = new SmoothingFunctionEpsilon();
        final BLEU blue = new BLEU(epsilon);

        double[] weights = { 0.3, 0.7 }; // Uniform weights for unigram and bigram
        final double score = blue.calculate(commonReferences, commonHypothesis, 2, weights);

        assertEquals(0.3, score, 0.05);
    }

    @Test
    @DisplayName("Calculate BLEU for simple inputs/hypotheses (no smoothing, weights)")
    void calculateSentenceNoSmoothingWeights() {
        final BLEU blue = new BLEU();

        double[] weights = { 0.3, 0.7 }; // Uniform weights for unigram and bigram
        final double score = blue.calculate(commonReferences, commonHypothesis, 2, weights);

        assertEquals(0.3, score, 0.05);
    }

    @Test
    @DisplayName("Calculate BLEU for simple inputs/hypotheses (no smoothing, no weights)")
    void calculateSentenceNoSmoothingNoWeights() {
        final BLEU blue = new BLEU();

        final double score = blue.calculate(commonReferences, commonHypothesis, 2);

        assertEquals(0.35, score, 0.05);
    }

    @Test
    @DisplayName("Test brevity penalty A")
    void brevityPenalty() {
        final List<List<String>> references = Arrays.asList(
                Arrays.asList("a", "a", "a", "a", "a", "a", "a", "a", "a", "a", "a"), // 11 'a's
                Arrays.asList("a", "a", "a", "a", "a", "a", "a", "a") // 8 'a's
        );

        final List<String> hypothesis = Arrays.asList("a", "a", "a", "a", "a", "a", "a"); // 7 'a's
        final int hyp_len = hypothesis.size();

        final int closest_ref_len = NLPUtils.closestReferenceLength(references, hyp_len);
        assertEquals(
                0.8669, BLEU.brevityPenalty(closest_ref_len, hyp_len), 0.0001);

    }

    @Test
    @DisplayName("Test brevity penalty B")
    void brevityPenaltyB() {
        // Create a list of references, where each reference is a list containing repeated "a" strings
        final List<List<String>> references = new ArrayList<>();
        references.add(Collections.nCopies(11, "a"));
        references.add(Collections.nCopies(8, "a"));
        references.add(Collections.nCopies(6, "a"));
        references.add(Collections.nCopies(7, "a"));

        // Create a hypothesis list containing repeated "a" strings
        final List<String> hypothesis = Collections.nCopies(7, "a");

        final int hyp_len = hypothesis.size();
        final int closest_ref_len = NLPUtils.closestReferenceLength(references, hyp_len);
        assertEquals(1.0, BLEU.brevityPenalty(closest_ref_len, hyp_len), 0.0001);
    }

    @Test
    @DisplayName("Test BLEU score for zero matches")
    void zeroMatches() {
        final List<String> references = Arrays.asList(uncommon.split(" "));
        final String hypothesis = "John loves Mary";

        final BLEU bleuCalculator = new BLEU();

        for (int n = 1; n <= hypothesis.length(); n++) {
            double score = bleuCalculator.calculate(references, hypothesis, n);
            assertEquals(0.0, score, 0.00001, "The score should be zero for no matches");
        }
    }

    @Test
    @DisplayName("Test BLEU score for full matches")
    void fullMatches() {
        final List<String> references = List.of(uncommon, uncommon, uncommon, uncommon);

        final BLEU bleuCalculator = new BLEU();

        for (int n = 1; n <= 10; n++) {
            double score = bleuCalculator.calculate(references, uncommon, n);
            assertEquals(1.0, score, 0.00001, "The score should be one for full matches");
        }
    }

    @Test
    @DisplayName("BLEU validation A (BLEU-2)")
    void validationBLEU2() {
        final BLEU bleuCalculator = new BLEU();
        assertEquals(0.7453, bleuCalculator.calculate(validationReference, validationHypothesisA, 2), 0.05);
    }

    @Test
    @DisplayName("BLEU validation A (BLEU-3)")
    void validationBLEU3() {
        final BLEU bleuCalculator = new BLEU();
        assertEquals(0.6240, bleuCalculator.calculate(validationReference, validationHypothesisA, 3), 0.05);
    }

    @Test
    @DisplayName("BLEU validation A (BLEU-4)")
    void validationBLEU4() {
        final BLEU bleuCalculator = new BLEU();
        assertEquals(0.5045, bleuCalculator.calculate(validationReference, validationHypothesisA, 4), 0.02);
    }

    @Test
    @DisplayName("BLEU validation A (BLEU-5)")
    void validationBLEU5() {
        final BLEU bleuCalculator = new BLEU();
        assertEquals(0.3920, bleuCalculator.calculate(validationReference, validationHypothesisA, 5), 0.02);
    }

    @Test
    @DisplayName("Modified precision with repetition")
    @Disabled("TODO")
    void modifiedPrecisionWithRepetition() {
        final List<String> references = List.of("the cat is on the mat", "there is a cat on the mat");
        final String hypothesis = "the the the the the the the";
        final BLEU bleu = new BLEU();
        assertEquals(0.2857, bleu.modifiedPrecision(references, hypothesis, 1), 0.05);
    }

    @Test
    @DisplayName("Modified precision with single word hypothesis")
    void modifiedPrecisionWithSingleWordHypothesis() {
        final List<String> references = List.of(
                "It is a guide to action that ensures that the military will forever heed Party commands",
                "It is the guiding principle which guarantees the military forces always being under the command of the Party",
                "It is the practical guide for the army always to heed the directions of the party");
        final String hypothesis = "of the";
        final BLEU bleu = new BLEU();
        assertEquals(1.0, bleu.modifiedPrecision(references, hypothesis, 1), 0.0001);
        assertEquals(1.0, bleu.modifiedPrecision(references, hypothesis, 2), 0.0001);
    }

    @Test
    @DisplayName("Modified precision with normal hypothesis")
    void modifiedPrecisionWithNormalHypothesis() {
        final double threshold = 0.05;
        final List<String> references = List.of(
                "It is a guide to action that ensures that the military will forever heed Party commands",
                "It is the guiding principle which guarantees the military forces always being under the command of the Party",
                "It is the practical guide for the army always to heed the directions of the party");

        final String hypothesis1 = "It is a guide to action which ensures that the military always obeys the commands of the party";
        final BLEU bleu = new BLEU();
        assertEquals(0.9444, bleu.modifiedPrecision(references, hypothesis1, 1), threshold);
        assertEquals(0.5882, bleu.modifiedPrecision(references, hypothesis1, 2), threshold);

        String hypothesis2 = "It is to insure the troops forever hearing the activity guidebook that party direct";
        assertEquals(0.5714, bleu.modifiedPrecision(references, hypothesis2, 1), threshold);
        assertEquals(0.0769, bleu.modifiedPrecision(references, hypothesis2, 2), threshold);
    }

    @Test
    @DisplayName("Test BLEU corpus")
    void corpusBLEU() {
        final BLEU bleu = new BLEU();

        final double[] weights = BLEU.createUniformWeights(4);

        final List<List<String>> list_of_references = List.of(List.of(ref1a, ref1b, ref1c), List.of(ref2a));
        final List<String> hypotheses = List.of(hyp1, hyp2);
        // Assert with a delta for floating-point comparisons
        assertEquals(0.5920, bleu.calculateCorpus(list_of_references, hypotheses, weights), 0.01);

        // individual scores
        double score1 = bleu.calculate(List.of(ref1a, ref1b, ref1c), hyp1);
        double score2 = bleu.calculate(List.of(ref2a), hyp2);
        assertEquals(0.6223, (score1 + score2) / 2.0, 0.05);
    }

    @Test
    @DisplayName("Test BLEU corpus with different weights")
    void corpusBLEUWithDifferentWeights() {
        final BLEU bleu = new BLEU();

        // Custom weights for testing
        final double[] weights = { 0.1, 0.3, 0.5, 0.1 };

        final List<List<String>> list_of_references = List.of(List.of(ref1a, ref1b, ref1c), List.of(ref2a));
        final List<String> hypotheses = List.of(hyp1, hyp2);

        // Assert with a delta for floating-point comparisons
        assertEquals(0.5818, bleu.calculateCorpus(list_of_references, hypotheses, weights), 0.01);
    }

    @Test
    @DisplayName("Test BLEU corpus with multiple weights")
    void corpusBLEUWithMultipleWeightSets() {
        final BLEU bleu = new BLEU();

        // Multiple sets of weights
        final double[][] weightSets = {
                { 0.5, 0.5 },
                { 0.333, 0.333, 0.334 },
                { 0.25, 0.25, 0.25, 0.25 },
                { 0.2, 0.2, 0.2, 0.2, 0.2 }
        };

        // Expected BLEU scores for the different weight sets
        final double[] expectedBLEUScores = { 0.8242, 0.7067, 0.5920, 0.4719 };

        final List<List<String>> references = List.of(List.of(ref1a, ref1b, ref1c), List.of(ref2a));
        final List<String> hypotheses = List.of(hyp1, hyp2);

        // Calculate BLEU scores for each set of weights
        for (int i = 0; i < weightSets.length; i++) {
            final double actualBLEUScore = bleu.calculateCorpus(references, hypotheses, weightSets[i]);
            assertEquals(expectedBLEUScores[i], actualBLEUScore, 0.02);
        }
    }
}
