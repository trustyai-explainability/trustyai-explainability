package org.kie.trustyai.metrics.language.wer;

import java.util.Arrays;
import java.util.List;

import org.kie.trustyai.metrics.language.AbstractNLPPerformanceMetric;
import org.kie.trustyai.metrics.language.utils.AlignedTokenSequences;
import org.kie.trustyai.metrics.language.utils.TokenSequenceAligner;
import org.kie.trustyai.metrics.language.utils.TokenSequenceAlignmentCounters;

import opennlp.tools.tokenize.Tokenizer;

public class WordErrorRate extends AbstractNLPPerformanceMetric<WordErrorRateResult, String> {

    public WordErrorRate() {
        super();
    }

    public WordErrorRate(Tokenizer tokenizer) {
        super(tokenizer);
    }

    @Override
    public WordErrorRateResult calculate(String reference, String hypothesis) {
        return calculate(
                Arrays.asList(this.getTokenizer().tokenize(reference)),
                Arrays.asList(this.getTokenizer().tokenize(hypothesis)));
    }

    public WordErrorRateResult calculate(List<String> tokenizedReference, List<String> tokenizedInput) {

        AlignedTokenSequences alignedTokenSequences = TokenSequenceAligner.align(tokenizedReference, tokenizedInput);
        TokenSequenceAlignmentCounters alignmentCounters = alignedTokenSequences.getAlignmentCounters();
        double wer = (alignmentCounters.substitutions + alignmentCounters.deletions + alignmentCounters.insertions) / (float) tokenizedReference.size();
        return new WordErrorRateResult(
                wer,
                alignedTokenSequences.getAlignedReferenceVisualization(),
                alignedTokenSequences.getAlignedInputVisualization(),
                alignedTokenSequences.getAlignedLabelVisualization(),
                alignmentCounters);
    }

}
