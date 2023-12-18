package org.kie.trustyai.metrics.language.levenshtein;

import org.apache.commons.text.StringTokenizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LevenshteinCommonTest {

    public static final double TOLERANCE = 1e-5;
    public static List<String> references = List.of(
            "This is the test reference, to which I will compare alignment against.",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur condimentum velit id velit posuere dictum. Fusce euismod tortor massa, nec euismod sapien laoreet non. Donec vulputate mi velit, eu ultricies nibh iaculis vel. Aenean posuere urna nec sapien consectetur, vitae porttitor sapien finibus. Duis nec libero convallis lectus pharetra blandit ut ac odio. Vivamus nec dui quis sem convallis pulvinar. Maecenas sodales sollicitudin leo a faucibus.",
            "The quick red fox jumped over the lazy brown dog",
            "i love cold pizza");

    public static List<String> inputs = List.of(
            "I'm a hypothesis reference, from which the aligner  will compare against.",
            "Lorem ipsum sit amet, consectetur adipiscing elit. Curabitur condimentum velit id velit posuere dictum. Fusce blandit euismod tortor massa, nec euismod sapien blandit laoreet non. Donec vulputate mi velit, eu ultricies nibh iaculis vel. Aenean posuere urna nec sapien consectetur, vitae porttitor sapien finibus. Duis nec libero convallis lectus pharetra blandit ut ac odio. Vivamus nec dui quis sem convallis pulvinar. Maecenas sodales sollicitudin leo a faucibus.",
            "dog brown lazy the over jumped fox red quick The",
            "i love pizza");

    public static List<Double> groundTruthWERsCommonsTokenizer = List.of(
            8 / 12.,
            3 / 66.,
            1.0);

    // nlp tokenizer creates a different number of tokens and thus produces different answers
    public static List<Double> groundTruthWERsNLPTokenizer = List.of(
            9 / 14.,
            3 / 78.,
            1.0);

    // nlp whitespace tokenizer creates a different number of tokens and thus produces different answers
    public static List<Double> groundTruthsWhitespaceTokenizer = List.of(
            8 / 14.,
            3 / 78.,
            10 / 10.);

    public void testAll(List<String> tokenizedReference, List<String> tokenizedHypotesis, double expectedWER, double expectedMER, double expectedWIL, double expectedWIP, double tolerance) {
        // WER test
        final ErrorRateResult werResult = new WordErrorRate().calculate(tokenizedReference, tokenizedHypotesis);
        assertEquals(expectedWER, werResult.getValue(), "Expected WER of " + expectedWER + ", got " + werResult.getValue());
        // MER test
        final ErrorRateResult merResult = new MatchErrorRate().calculate(tokenizedReference, tokenizedHypotesis);
        assertEquals(expectedMER, merResult.getValue(), tolerance,"Expected MER of " + expectedMER + ", got " + merResult.getValue());
        // WIL test
        final ErrorRateResult wilResult = new WordInformationLost().calculate(tokenizedReference, tokenizedHypotesis);
        assertEquals(expectedWIL, wilResult.getValue(), tolerance, "Expected WIL of " + expectedWIL + ", got " + wilResult.getValue());
        // WIP test
        final ErrorRateResult wipResult = new WordInformationPreserved().calculate(tokenizedReference, tokenizedHypotesis);
        assertEquals(expectedWIP, wipResult.getValue(), tolerance,"Expected WIP of " + expectedWIP + ", got " + wipResult.getValue());
    }

    @Test
    void testEqualReferenceHypothesis() {
        final List<String> tokenizedReference = new StringTokenizer("X").getTokenList();
        final List<String> tokenizedHypotesis = new StringTokenizer("X").getTokenList();
        testAll(tokenizedReference, tokenizedHypotesis, 0.0, 0.0, 0.0, 1.0, TOLERANCE);
    }

    @Test
    void testRepeatedHypothesis() {
        final List<String> tokenizedReference = new StringTokenizer("X").getTokenList();
        final List<String> tokenizedHypotesis = new StringTokenizer("X X Y Y").getTokenList();
        testAll(tokenizedReference, tokenizedHypotesis, 3., 0.75, 0.75, 0.25, TOLERANCE);
    }

    @Test
    void testOverlap() {
        final List<String> tokenizedReference = new StringTokenizer("X Y Z").getTokenList();
        final List<String> tokenizedHypotesis = new StringTokenizer("X Z").getTokenList();
        testAll(tokenizedReference, tokenizedHypotesis, 1 / 3., 1/3., 1/3., 1.0 - 1/3., TOLERANCE);
    }
}
