package org.kie.trustyai.metrics.language.utils;

import org.apache.commons.text.StringTokenizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenSequenceAlignerTest {

    @Test
    public void test() {
        String testRef = "This is the test reference, to which I will compare alignment against.";
        String testHyp = "I'm a hypothesis reference, from which the aligner  will compare against";

        AlignedTokenSequences alignedSequences = TokenSequenceAligner.align(
                new StringTokenizer(testRef).getTokenList(),
                new StringTokenizer(testHyp).getTokenList());

        assertEquals(alignedSequences.getAlignedInput().size(), alignedSequences.getAlignedReference().size());
    }

}
