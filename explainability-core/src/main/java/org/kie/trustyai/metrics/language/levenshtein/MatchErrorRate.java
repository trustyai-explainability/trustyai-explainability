package org.kie.trustyai.metrics.language.levenshtein;

import java.util.List;

import org.kie.trustyai.metrics.language.AbstractLevenshteinMetric;
import org.kie.trustyai.metrics.language.distance.Levenshtein;
import org.kie.trustyai.metrics.language.distance.LevenshteinCounters;

import opennlp.tools.tokenize.Tokenizer;

/**
 * Match Error Rate (MER)
 * Morris, Andrew and Maier, Viktoria and Green, Phil (2004), "From WER and RIL to MER and WIL: improved evaluation measures for connected speech recognition."
 */
public class MatchErrorRate extends AbstractLevenshteinMetric<ErrorRateResult> {

    public MatchErrorRate() {
        super();
    }

    public MatchErrorRate(Tokenizer tokenizer) {
        super(tokenizer);
    }

    public ErrorRateResult calculate(List<String> tokenizedReference, List<String> tokenizedHypothesis) {

        final LevenshteinCounters counters = Levenshtein.calculateToken(tokenizedReference, tokenizedHypothesis).getCounters();

        final double S = counters.getSubstitutions();
        final double D = counters.getDeletions();
        final double I = counters.getInsertions();
        final double H = counters.getCorrect();

        final double K = (S + D + I);

        double mer = K / (H + K);

        return new ErrorRateResult(mer, counters);
    }

}
