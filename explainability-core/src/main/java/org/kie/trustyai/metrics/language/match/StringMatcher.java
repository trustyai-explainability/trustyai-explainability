package org.kie.trustyai.metrics.language.match;

import org.apache.commons.lang3.NotImplementedException;
import org.kie.trustyai.metrics.language.wer.WordErrorRate;

import opennlp.tools.tokenize.Tokenizer;

public class StringMatcher {
    private StringMatcher() {
        throw new IllegalStateException("Static utility class");
    }

    /**
     * Compare two strings for exact equality
     * 
     * @param reference: The first string to compare
     * @param input: The second string to compare
     * @return reference.equals(input)
     */
    public static boolean exactMatch(String reference, String input) {
        return reference.equals(input);
    }

    /**
     * Compare two strings for a fuzzy match: if their Word Error Rate (WER) is below the threshold, return true
     *
     * @param reference: The first string to compare
     * @param input: The second string to compare
     * @param threshold: The threshold WER, such that all WER<threshold are considered a "match"
     * @return WordErrorRate(reference, input) < threshold
     */
    public static boolean werMatch(String reference, String input, double threshold) {
        System.out.println(WordErrorRate.calculate(reference, input).getWordErrorRate());
        return WordErrorRate.calculate(reference, input).getWordErrorRate() < threshold;
    }

    /**
     * Compare two strings for a fuzzy match: if their Word Error Rate (WER) is below the threshold, return true
     *
     * @param reference: The first string to compare
     * @param input: The second string to compare
     * @param threshold: The threshold WER, such that all WER<threshold are considered a "match"
     * @param tokenizer: The tokenizer to use in the Word Error Rate calculation
     * @return WordErrorRate(reference, input) < threshold
     */
    public static boolean werMatch(String reference, String input, double threshold, Tokenizer tokenizer) {
        return WordErrorRate.calculate(reference, input, tokenizer).getWordErrorRate() < threshold;
    }

    /**
     * Compare two strings for a fuzzy match: if their BLEU score is *above* the threshold, return true
     *
     * @param reference: The first string to compare
     * @param input: The second string to compare
     * @param threshold: The threshold BLEU, such that all BLEU>threshold are considered a "match"
     * @return BLEU(reference, input) > threshold
     */
    public static boolean bleuMatch(String reference, String input, double threshold) {
        //todo
        throw new NotImplementedException("Not yet implemented");
    }

}
