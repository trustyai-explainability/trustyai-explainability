package org.kie.trustyai.metrics.language.wer;

import org.kie.trustyai.metrics.language.utils.TokenSequenceAlignmentCounters;

public class WordErrorRateResult {
    private final double wordErrorRate;
    private final String alignedReferenceString;
    private final String alignedInputString;
    private final String alignedLabelString;
    private final TokenSequenceAlignmentCounters alignmentCounters;

    public WordErrorRateResult(double wordErrorRate, String alignedReferenceString, String alignedInputString, String alignedLabelString, TokenSequenceAlignmentCounters alignmentCounters) {
        this.wordErrorRate = wordErrorRate;
        this.alignedReferenceString = alignedReferenceString;
        this.alignedInputString = alignedInputString;
        this.alignedLabelString = alignedLabelString;
        this.alignmentCounters = alignmentCounters;
    }

    public double getWordErrorRate() {
        return wordErrorRate;
    }

    public String getAlignedReferenceString() {
        return alignedReferenceString;
    }

    public String getAlignedInputString() {
        return alignedInputString;
    }

    public TokenSequenceAlignmentCounters getAlignmentCounters() {
        return alignmentCounters;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("Word Error Rate: ")
                .append(wordErrorRate).append(System.getProperty("line.separator"))
                .append("Reference: ").append(alignedReferenceString).append(System.getProperty("line.separator"))
                .append("    Input: ").append(alignedInputString).append(System.getProperty("line.separator"))
                .append("   Labels: ").append(alignedLabelString)
                .append(System.getProperty("line.separator"))
                .append(alignmentCounters).toString();
    }
}
