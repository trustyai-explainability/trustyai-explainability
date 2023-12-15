package org.kie.trustyai.metrics.language.match;

import java.util.List;

import org.kie.trustyai.metrics.language.AbstractNLPPerformanceMetric;
import org.kie.trustyai.metrics.language.bleu.BLEU;
import org.kie.trustyai.metrics.language.levenshtein.WordErrorRate;

import opennlp.tools.tokenize.Tokenizer;

public class FuzzyMatch extends AbstractNLPPerformanceMetric<Boolean, String> {

    public FuzzyMatch() {
        super();
    }

    public FuzzyMatch(Tokenizer tokenizer) {
        super(tokenizer);
    }

    /**
     * Compare two strings for exact equality
     * 
     * @param reference: The first string to compare
     * @param input: The second string to compare
     * @return reference.equals(input)
     */
    @Override
    public Boolean calculate(String reference, String input) {
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
    public boolean calculateWER(String reference, String input, double threshold) {
        return new WordErrorRate(this.getTokenizer()).calculate(reference, input).getValue() < threshold;
    }

    /**
     * Compare two strings for a fuzzy match: if their BLEU score is *above* the threshold, return true
     *
     * @param references: The set of string to compare
     * @param input: The second string to compare
     * @param threshold: The threshold BLEU, such that all BLEU>threshold are considered a "match"
     * @return BLEU(reference, input) > threshold
     */
    public boolean calculateBLEU(List<String> references, String input, double threshold) {
        return new BLEU().calculate(references, input) > threshold;
    }

    /**
     * Compare two strings for a fuzzy match: if their BLEU score is *above* the threshold, return true
     *
     * @param references: The set of string to compare
     * @param input: The second string to compare
     * @param threshold: The threshold BLEU, such that all BLEU>threshold are considered a "match"
     * @param maxNgram: The maximum n-gram order to pass to BLEU.
     * @return BLEU(reference, input) > threshold
     */
    public boolean calculateBLEU(List<String> references, String input, double threshold, int maxNgram) {
        return new BLEU().calculate(references, input, maxNgram) > threshold;
    }

    /**
     * Compare two strings for a fuzzy match: if their BLEU score is *above* the threshold, return true
     *
     * @param references: The set of string to compare
     * @param input: The second string to compare
     * @param threshold: The threshold BLEU, such that all BLEU>threshold are considered a "match"
     * @param maxNgram: The maximum n-gram order to pass to BLEU.
     *        * @param weights The weights for each n-gram order to pass to BLEU.
     * @return BLEU(reference, input) > threshold
     */
    public boolean calculateBLEU(List<String> references, String input, double threshold, int maxNgram, double[] weights) {
        return new BLEU().calculate(references, input, maxNgram, weights) > threshold;
    }
}
