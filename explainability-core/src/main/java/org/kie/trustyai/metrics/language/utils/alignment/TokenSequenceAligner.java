package org.kie.trustyai.metrics.language.utils.alignment;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/*
Reproduced here is the license terms and authors of the Sphinx-4 package
 https://github.com/cmusphinx/sphinx4/ which contains the NISTAlign algorithm upon which this class is based

Copyright 1999-2015 Carnegie Mellon University.
Portions Copyright 2002-2008 Sun Microsystems, Inc.
Portions Copyright 2002-2008 Mitsubishi Electric Research Laboratories.
Portions Copyright 2013-2015 Alpha Cephei, Inc.

All Rights Reserved.  Use is subject to license terms.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

3. Original authors' names are not deleted.

4. The authors' names are not used to endorse or promote products
   derived from this software without specific prior written
   permission.

This work was supported in part by funding from the Defense Advanced
Research Projects Agency and the National Science Foundation of the
United States of America, the CMU Sphinx Speech Consortium, and
Sun Microsystems, Inc.

CARNEGIE MELLON UNIVERSITY, SUN MICROSYSTEMS, INC., MITSUBISHI
ELECTRONIC RESEARCH LABORATORIES AND THE CONTRIBUTORS TO THIS WORK
DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL
CARNEGIE MELLON UNIVERSITY, SUN MICROSYSTEMS, INC., MITSUBISHI
ELECTRONIC RESEARCH LABORATORIES NOR THE CONTRIBUTORS BE LIABLE FOR
ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

Sphinx-4 Team:
Evandro Gouvea, CMU (developer and speech advisor)
Peter Gorniak, MIT (developer)
Philip Kwok, Sun Labs (developer)
Paul Lamere, Sun Labs (design/technical lead)
Beth Logan, HP (speech advisor)
Pedro Moreno, Google (speech advisor)
Bhiksha Raj, MERL (design lead)
Mosur Ravishankar, CMU (speech advisor)
Bent Schmidt-Nielsen, MERL (speech advisor)
Rita Singh, CMU/MIT (design/speech advisor)
JM Van Thong, HP (speech advisor)
Willie Walker, Sun Labs (overall lead)
Manfred Warmuth, USCS (speech advisor)
Joe Woelfel, MERL (developer and speech advisor)
Peter Wolf, MERL (developer and speech advisor)
 */
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

public class TokenSequenceAligner {
    // struct to hold the various alignment operations
    private enum Alignment {
        OK,
        SUBSTITUTION,
        INSERTION,
        DELETION
    }

    // constants for alignment graph edge weighting
    private static final int MAX_PENALTY = 1000000;
    private static final int SUBSTITUTION_PENALTY = 100;
    private static final int INSERTION_PENALTY = 75;
    private static final int DELETION_PENALTY = 75;

    // constants for alignment visualization functions
    private static final String MISSING_TOKEN_FILLER = "*";
    private static final String TOKEN_PADDER = " ";
    private static final String TOKEN_SEPARATOR = " |";

    /**
     * Align two token sequences for use in comparison metrics
     * Based on the NISTAlign method https://github.com/cmusphinx/sphinx4/blob/master/sphinx4-core/src/main/java/edu/cmu/sphinx/util/NISTAlign.java
     *
     * @param reference: First list of tokens, by which top base the alignment on
     * @param hypothesis Second list of tokens, to align to the reference
     *
     * @return an AlignedTokenSequences object that contains the alignments and string representations
     */

    public static AlignedTokenSequences align(List<String> reference, List<String> hypothesis) {
        return align(reference, hypothesis, String::equals);
    }

    /**
     * Align two token sequences for use in comparison metrics
     * Based on the NISTAlign method https://github.com/cmusphinx/sphinx4/blob/master/sphinx4-core/src/main/java/edu/cmu/sphinx/util/NISTAlign.java
     *
     * @param reference: First list of tokens, by which top base the alignment on
     * @param hypothesis Second list of tokens, to align to the reference
     * @param comparator a function of two string that returns true when they match, false otherwise
     *
     * @return an AlignedTokenSequences object that contains the alignments and string representations
     */
    public static AlignedTokenSequences align(List<String> reference, List<String> hypothesis, BiPredicate<String, String> comparator) {
        TokenSequenceAlignmentCounters counters = new TokenSequenceAlignmentCounters();
        Alignment[][] backtraceGraph = createBacktraceGraph(reference, hypothesis, comparator);
        List<Alignment> alignmentOperations = traverseBacktrace(backtraceGraph, counters);
        counters.correct = reference.size() - counters.deletions - counters.substitutions;
        return new AlignedTokenSequences(produceAlignment(alignmentOperations, reference, hypothesis), counters);
    }

    // set values of the penalty and backtrace table depending on penalty and alignment code
    private static BiConsumer<Integer, Alignment> penaltyCurrier(int i, int j, AtomicInteger minPenalty, int[][] penaltyGraph, Alignment[][] backtraceGraph) {
        return (penalty, alignment) -> {
            if (penalty < (minPenalty.get())) {
                minPenalty.set(penalty);
                penaltyGraph[i][j] = penalty;
                backtraceGraph[i][j] = alignment;
            }
        };
    }

    // create weighted graph describing the various alignment operation (DEL, INS, SUB, etc) paths that could transmute the hypothesis sequence
    // into the reference string
    private static Alignment[][] createBacktraceGraph(List<String> reference, List<String> hypothesis, BiPredicate<String, String> comparator) {
        // set up graphs
        // rows are words in reference
        // columns are words in seq-to-align
        // position i,j is the edge between reference token i and hypothesis token j

        int[][] penaltyGraph = new int[reference.size() + 1][hypothesis.size() + 1];
        Alignment[][] backtraceGraph = new Alignment[reference.size() + 1][hypothesis.size() + 1];
        backtraceGraph[0][0] = Alignment.OK;

        // set up counters
        int penalty;
        AtomicInteger minPenalty = new AtomicInteger();

        for (int i = 1; i <= reference.size(); i++) {
            penaltyGraph[i][0] = DELETION_PENALTY * i;
            backtraceGraph[i][0] = Alignment.DELETION;

            for (int j = 1; j <= hypothesis.size(); j++) {
                penaltyGraph[0][j] = INSERTION_PENALTY * j;
                backtraceGraph[0][j] = Alignment.INSERTION;
                minPenalty.set(MAX_PENALTY);

                // get setter function for matrices
                BiConsumer<Integer, Alignment> penaltySetter = penaltyCurrier(i, j, minPenalty, penaltyGraph, backtraceGraph);

                // assume we have a deletion
                penalty = penaltyGraph[i - 1][j] + DELETION_PENALTY;
                penaltySetter.accept(penalty, Alignment.DELETION);

                // do the words match?
                if (comparator.test(reference.get(i - 1), hypothesis.get(j - 1))) {
                    penalty = penaltyGraph[i - 1][j - 1];
                    penaltySetter.accept(penalty, Alignment.OK);
                } else {
                    penalty = penaltyGraph[i - 1][j - 1] + SUBSTITUTION_PENALTY;
                    penaltySetter.accept(penalty, Alignment.SUBSTITUTION);

                }

                penalty = penaltyGraph[i][j - 1] + INSERTION_PENALTY;
                penaltySetter.accept(penalty, Alignment.INSERTION);

            }
        }

        return backtraceGraph;
    }

    // traverse backtrace graph from lower-right, i.e., last word of both sequences, and return path with minimal penalty
    private static List<Alignment> traverseBacktrace(Alignment[][] backtraceTable, TokenSequenceAlignmentCounters counters) {
        List<Alignment> traversal = new ArrayList<>();

        // recover size of reference and to-align sequences from shape of table
        // this avoids having to pass those lists into this function
        int i = backtraceTable.length - 1;
        int j = backtraceTable[0].length - 1;

        // traverse backtrace from lower-right, i.e., last word of both sequences
        while ((i >= 0) && (j >= 0)) {
            traversal.add(backtraceTable[i][j]);
            switch (backtraceTable[i][j]) {
                case OK: //sequences remain aligned, move to previous word in both seqs
                    i--;
                    j--;
                    break;
                case SUBSTITUTION: //sequences remain aligned, move to previous word in both seqs
                    i--;
                    j--;
                    counters.substitutions++;
                    break;
                case INSERTION: // extra token in toAlign, move one space directly left;
                    j--;
                    counters.insertions++;
                    break;
                case DELETION: // missing token in toAlign, move one space directly up;
                    i--;
                    counters.deletions++;
                    break;
            }
        }
        return traversal;
    }

    // follow the backtrace traversal path to align the two sequences
    private static Pair<List<String>, List<String>> produceAlignment(List<Alignment> alignmentOperations, List<String> reference, List<String> hypothesis) {
        ListIterator<String> referenceIterator = reference.listIterator();
        ListIterator<String> hypothesisIterator = hypothesis.listIterator();
        String referenceWord;
        String hypothesisWord;

        List<String> alignedReference = new ArrayList<>();
        List<String> alignedHypothesis = new ArrayList<>();

        // walk through operations in reverse
        for (int i = alignmentOperations.size() - 2; i >= 0; i--) {
            Alignment alignmentOperation = alignmentOperations.get(i);

            hypothesisWord = null;
            referenceWord = null;
            if (alignmentOperation == Alignment.INSERTION) {
                hypothesisWord = hypothesisIterator.next();
            } else if (alignmentOperation == Alignment.DELETION) {
                referenceWord = referenceIterator.next();
            } else {
                referenceWord = referenceIterator.next();
                hypothesisWord = hypothesisIterator.next();
            }

            alignedReference.add(referenceWord);
            alignedHypothesis.add(hypothesisWord);
        }

        return Pair.of(alignedReference, alignedHypothesis);
    }

    /**
     * Given the outputs of TokenSequenceAligner.align, generate aligned string representations of the two inputted sequences
     *
     * @param alignedReference the alignedReference sequence
     * @param alignedHypothesis the alignedHypothesis sequence
     * @return an aligned Pair of ReferenceString, InputString
     */
    protected static Triple<String, String, String> toStrings(List<String> alignedReference, List<String> alignedHypothesis) {
        if (alignedReference.size() != alignedHypothesis.size()) {
            throw new IllegalArgumentException(String.format(
                    "Aligned Sequence Visualizer is designed to receive the outputs of TokenSequenceAligner.align, therefore" +
                            "alignedReference.size() must equals alignedHypothesis.size(), but got %d versus %d.",
                    alignedReference.size(),
                    alignedHypothesis.size()));
        }

        StringJoiner processedReferenceSequence = new StringJoiner(TOKEN_PADDER);
        StringJoiner processedHypothesisSequence = new StringJoiner(TOKEN_PADDER);
        StringJoiner processedLabelSequence = new StringJoiner(TOKEN_PADDER);

        for (int i = 0; i < alignedReference.size(); i++) {
            String referenceWord = alignedReference.get(i);
            String hypothesisWord = alignedHypothesis.get(i);

            String processedReferenceWord = referenceWord;
            String processedHypothesisWord = hypothesisWord;

            if (referenceWord == null) {
                processedReferenceWord = MISSING_TOKEN_FILLER.repeat(hypothesisWord.length());
            } else if (hypothesisWord == null) {
                processedHypothesisWord = MISSING_TOKEN_FILLER.repeat(referenceWord.length());
            } else if (referenceWord.length() > hypothesisWord.length()) {
                processedHypothesisWord += TOKEN_PADDER.repeat(referenceWord.length() - hypothesisWord.length());
            } else if (hypothesisWord.length() > referenceWord.length()) {
                processedReferenceWord += TOKEN_PADDER.repeat(hypothesisWord.length() - processedReferenceWord.length());
            } else {
                ;
            }

            String processedLabelWord;
            int wordLen = processedHypothesisWord.length();
            if (processedReferenceWord.equals(processedHypothesisWord)) {
                processedLabelWord = "C" + TOKEN_PADDER.repeat(wordLen - 1);
            } else if (processedReferenceWord.contains(MISSING_TOKEN_FILLER)) {
                processedLabelWord = "I" + TOKEN_PADDER.repeat(wordLen - 1);
            } else if (processedHypothesisWord.contains(MISSING_TOKEN_FILLER)) {
                processedLabelWord = "D" + TOKEN_PADDER.repeat(wordLen - 1);
            } else {
                processedLabelWord = "S" + TOKEN_PADDER.repeat(wordLen - 1);
            }

            processedReferenceSequence.add(processedReferenceWord).add(TOKEN_SEPARATOR);
            processedHypothesisSequence.add(processedHypothesisWord).add(TOKEN_SEPARATOR);
            processedLabelSequence.add(processedLabelWord).add(TOKEN_SEPARATOR);
        }

        return Triple.of(
                processedReferenceSequence.toString(),
                processedHypothesisSequence.toString(),
                processedLabelSequence.toString());
    }

}
