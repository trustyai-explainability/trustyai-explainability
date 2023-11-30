package org.kie.trustyai.metrics.language.utils.alignment;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class AlignedTokenSequences {
    private final List<String> alignedReference;
    private final List<String> alignedHypothesis;
    private final String alignedReferenceVisualization;
    private final String alignedHypothesisVisualization;
    private final String alignedLabelVisualization;
    private final TokenSequenceAlignmentCounters alignmentCounters;

    public AlignedTokenSequences(Pair<List<String>, List<String>> alignedSequencePair, TokenSequenceAlignmentCounters alignmentCounters) {
        this.alignedReference = alignedSequencePair.getLeft();
        this.alignedHypothesis = alignedSequencePair.getRight();
        this.alignmentCounters = alignmentCounters;

        Triple<String, String, String> processedTriple = TokenSequenceAligner.toStrings(alignedReference, alignedHypothesis);
        alignedReferenceVisualization = processedTriple.getLeft();
        alignedHypothesisVisualization = processedTriple.getMiddle();
        alignedLabelVisualization = processedTriple.getRight();
    }

    public AlignedTokenSequences(List<String> alignedReference, List<String> alignedHypothesis, TokenSequenceAlignmentCounters alignmentCounters) {
        this.alignedReference = alignedReference;
        this.alignedHypothesis = alignedHypothesis;
        this.alignmentCounters = alignmentCounters;

        Triple<String, String, String> processedTriple = TokenSequenceAligner.toStrings(alignedReference, alignedHypothesis);
        alignedReferenceVisualization = processedTriple.getLeft();
        alignedHypothesisVisualization = processedTriple.getMiddle();
        alignedLabelVisualization = processedTriple.getRight();
    }

    public List<String> getAlignedReference() {
        return alignedReference;
    }

    public List<String> getAlignedHypothesis() {
        return alignedHypothesis;
    }

    public String getAlignedReferenceVisualization() {
        return alignedReferenceVisualization;
    }

    public String getAlignedHypothesisVisualization() {
        return alignedHypothesisVisualization;
    }

    public String getAlignedLabelVisualization() {
        return alignedLabelVisualization;
    }

    public TokenSequenceAlignmentCounters getAlignmentCounters() {
        return alignmentCounters;
    }
}
