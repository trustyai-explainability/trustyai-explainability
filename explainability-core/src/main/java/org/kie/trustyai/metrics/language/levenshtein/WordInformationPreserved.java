package org.kie.trustyai.metrics.language.levenshtein;

import java.util.List;

import org.kie.trustyai.metrics.language.AbstractLevenshteinMetric;
import org.kie.trustyai.metrics.language.distance.Levenshtein;
import org.kie.trustyai.metrics.language.distance.LevenshteinCounters;

import opennlp.tools.tokenize.Tokenizer;

/**
 * Word Information Preserved (WIP)
 */
public class WordInformationPreserved extends AbstractLevenshteinMetric<ErrorRateResult> {

    public WordInformationPreserved() {
        super();
    }

    public WordInformationPreserved(Tokenizer tokenizer) {
        super(tokenizer);
    }

    @Override
    public ErrorRateResult calculate(List<String> tokenizedReference, List<String> tokenizedHypothesis) {

        final LevenshteinCounters counters = Levenshtein.calculateToken(tokenizedReference, tokenizedHypothesis).getCounters();

        final double H = counters.getCorrect();
        final double refSize = tokenizedReference.size();
        final double hypSize = tokenizedHypothesis.size();

        final double wip = (H / refSize) * (H / hypSize);

        return new ErrorRateResult(wip, counters);
    }

}
