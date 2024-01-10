package org.kie.trustyai.metrics.language;

import java.util.Arrays;
import java.util.List;

import opennlp.tools.tokenize.Tokenizer;

public abstract class AbstractLevenshteinMetric<T> extends AbstractNLPPerformanceMetric<T, String> {

    protected AbstractLevenshteinMetric() {
        super();
    }

    protected AbstractLevenshteinMetric(Tokenizer tokenizer) {
        super(tokenizer);
    }

    /**
     * Calculate the Levenshtein-based distance metric, based on a reference string and hypothesis.
     * The supplied {@link Tokenizer} will be used. If none supplied, the default {@link opennlp.tools.tokenize.WhitespaceTokenizer} will be used.
     * 
     * @param reference The reference string
     * @param hypothesis The hypothesis string
     * @return The metric result type
     */
    @Override
    public T calculate(String reference, String hypothesis) {
        return calculate(
                Arrays.asList(this.getTokenizer().tokenize(reference)),
                Arrays.asList(this.getTokenizer().tokenize(hypothesis)));
    }

    /**
     * Calculate the Levenshtein-based distance metric, based on a pre-tokenized reference string and hypothesis.
     * 
     * @see <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">https://en.wikipedia.org/wiki/Levenshtein_distance</a>
     * @param tokenizedReference Pre-tokenized list of reference tokens
     * @param tokenizedHypothesis Pre-tokenized list of hypothesis tokens
     * @return The metric result type
     */
    public abstract T calculate(List<String> tokenizedReference, List<String> tokenizedHypothesis);

}
