package org.kie.trustyai.metrics.language.nlp;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.metrics.language.utils.NLPUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NLPUtilsTest {

    @Test
    @DisplayName("Closest reference length for a single reference")
    void closestReferenceLengthSingleReference() {
        final List<List<String>> references = Collections.singletonList(
                Arrays.asList("The", "quick", "brown", "fox")
        );
        final int hypothesisLength = 4;
        final int expected = 4;
        final int actual = NLPUtils.closestReferenceLength(references, hypothesisLength);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Closest reference length for a multiple reference")
    void closestReferenceLengthMultipleReferences() {
        final List<List<String>> references = Arrays.asList(
                Arrays.asList("The", "quick", "brown", "fox"),
                Arrays.asList("The", "fast", "brown", "fox", "jumps", "over"),
                Arrays.asList("A", "fast", "fox")
        );
        final int hypothesisLength = 5;
        final int expected = 4; // The closest reference length to hypothesis length 5 is 4
        final int actual = NLPUtils.closestReferenceLength(references, hypothesisLength);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Closest reference length for an empty reference")
    void closestReferenceLengthEmptyReferences() {
        final List<List<String>> references = Collections.emptyList();
        final int hypothesisLength = 5;
        final int expected = Integer.MAX_VALUE; // No references available
        final int actual = NLPUtils.closestReferenceLength(references, hypothesisLength);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Closest reference length for the same distance")
    void closestReferenceLengthSameDistance() {
        final List<List<String>> references = Arrays.asList(
                Arrays.asList("The", "quick"),
                Arrays.asList("The", "quick", "brown", "fox", "jumps")
        );
        final int hypothesisLength = 3;
        final int expected = 2; // When distances are the same, choose the smaller one
        final int actual = NLPUtils.closestReferenceLength(references, hypothesisLength);
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Closest reference length for empty references")
    void closestReferenceLengthAllEmptyReferences() {
        final List<List<String>> references = Arrays.asList(
                Collections.emptyList(),
                Collections.emptyList()
        );
        final int hypothesisLength = 0;
        final int expected = 0; // Closest length to 0 is 0, even if all references are empty
        final int actual = NLPUtils.closestReferenceLength(references, hypothesisLength);
        assertEquals(expected, actual);
    }

}