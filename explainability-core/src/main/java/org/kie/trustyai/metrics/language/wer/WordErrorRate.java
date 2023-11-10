package org.kie.trustyai.metrics.language.wer;

import java.util.Arrays;
import java.util.List;

import org.kie.trustyai.metrics.language.utils.alignment.AlignedTokenSequences;
import org.kie.trustyai.metrics.language.utils.alignment.TokenSequenceAligner;
import org.kie.trustyai.metrics.language.utils.alignment.TokenSequenceAlignmentCounters;

import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;

public class WordErrorRate {

    private WordErrorRate() {
        throw new IllegalStateException("Static class");
    }

    public static WordErrorRateResult calculate(String reference, String input) {
        return calculate(reference, input, SimpleTokenizer.INSTANCE);
    }

    public static WordErrorRateResult calculate(String reference, String input, Tokenizer tokenizer) {
        return WordErrorRate.calculate(
                Arrays.asList(tokenizer.tokenize(reference)),
                Arrays.asList(tokenizer.tokenize(input)));
    }

    public static WordErrorRateResult calculate(List<String> tokenizedReference, List<String> tokenizedInput) {
        AlignedTokenSequences alignedTokenSequences = TokenSequenceAligner.align(tokenizedReference, tokenizedInput);
        TokenSequenceAlignmentCounters alignmentCounters = alignedTokenSequences.getAlignmentCounters();
        double wer = (alignmentCounters.substitutions + alignmentCounters.deletions + alignmentCounters.insertions) / (float) tokenizedReference.size();
        return new WordErrorRateResult(wer, alignedTokenSequences.getAlignedReferenceVisualization(), alignedTokenSequences.getAlignedInputVisualization(), alignmentCounters);
    }

}
