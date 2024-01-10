package org.kie.trustyai.metrics.language.levenshtein;

import java.util.List;

import org.kie.trustyai.metrics.language.AbstractLevenshteinMetric;
import org.kie.trustyai.metrics.language.distance.Levenshtein;
import org.kie.trustyai.metrics.language.distance.LevenshteinCounters;

import opennlp.tools.tokenize.Tokenizer;

/**
 * Word Error Rate (WER)
 * Woodard, J.P. and Nelson, J.T., (1982), "An information theoretic measure of speech recognition performance"
 */
public class WordErrorRate extends AbstractLevenshteinMetric<ErrorRateResult> {

    public WordErrorRate() {
        super();
    }

    public WordErrorRate(Tokenizer tokenizer) {
        super(tokenizer);
    }

    public ErrorRateResult calculate(List<String> tokenizedReference, List<String> tokenizedHypothesis) {

        final LevenshteinCounters counters = Levenshtein.calculateToken(tokenizedReference, tokenizedHypothesis).getCounters();

        final double S = counters.getSubstitutions();
        final double D = counters.getDeletions();
        final double I = counters.getInsertions();
        final double N = tokenizedReference.size();

        final double wer = (S + D + I) / N;

        return new ErrorRateResult(wer, counters);
    }

}
