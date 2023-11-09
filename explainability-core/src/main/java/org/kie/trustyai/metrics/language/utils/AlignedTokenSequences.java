package org.kie.trustyai.metrics.language.utils;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

public class AlignedTokenSequences {
    private final List<String> alignedReference;
    private final List<String> alignedInput;
    private final String alignedReferenceVisualization;
    private final String alignedInputVisualization;
    private final TokenSequenceAlignmentCounters alignmentCounters;

    public AlignedTokenSequences(Pair<List<String>, List<String>> alignedSequencePair, TokenSequenceAlignmentCounters alignmentCounters) {
        this.alignedReference = alignedSequencePair.getLeft();
        this.alignedInput = alignedSequencePair.getRight();
        this.alignmentCounters = alignmentCounters;

        Pair<String, String> processedPair = TokenSequenceAligner.toStrings(alignedReference, alignedInput);
        alignedReferenceVisualization = processedPair.getLeft();
        alignedInputVisualization = processedPair.getRight();
    }

    public AlignedTokenSequences(List<String> alignedReference, List<String> alignedInput, TokenSequenceAlignmentCounters alignmentCounters) {
        this.alignedReference = alignedReference;
        this.alignedInput = alignedInput;
        this.alignmentCounters = alignmentCounters;

        Pair<String, String> processedPair = TokenSequenceAligner.toStrings(alignedReference, alignedInput);
        alignedReferenceVisualization = processedPair.getLeft();
        alignedInputVisualization = processedPair.getRight();
    }

    public List<String> getAlignedReference() {
        return alignedReference;
    }

    public List<String> getAlignedInput() {
        return alignedInput;
    }

    public String getAlignedReferenceVisualization() {
        return alignedReferenceVisualization;
    }

    public String getAlignedInputVisualization() {
        return alignedInputVisualization;
    }

    public TokenSequenceAlignmentCounters getAlignmentCounters() {
        return alignmentCounters;
    }
}
