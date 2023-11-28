package org.kie.trustyai.metrics.language.wer;

import java.util.List;

import org.apache.commons.text.StringTokenizer;
import org.junit.jupiter.api.Test;

import opennlp.tools.tokenize.SimpleTokenizer;

import static org.junit.jupiter.api.Assertions.*;

class WordErrorRateTest {
    List<String> references = List.of(
            "This is the test reference, to which I will compare alignment against.",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur condimentum velit id velit posuere dictum. Fusce euismod tortor massa, nec euismod sapien laoreet non. Donec vulputate mi velit, eu ultricies nibh iaculis vel. Aenean posuere urna nec sapien consectetur, vitae porttitor sapien finibus. Duis nec libero convallis lectus pharetra blandit ut ac odio. Vivamus nec dui quis sem convallis pulvinar. Maecenas sodales sollicitudin leo a faucibus.",
            "The quick red fox jumped over the lazy brown dog");

    List<String> inputs = List.of(
            "I'm a hypothesis reference, from which the aligner  will compare against.",
            "Lorem ipsum sit amet, consectetur adipiscing elit. Curabitur condimentum velit id velit posuere dictum. Fusce blandit euismod tortor massa, nec euismod sapien blandit laoreet non. Donec vulputate mi velit, eu ultricies nibh iaculis vel. Aenean posuere urna nec sapien consectetur, vitae porttitor sapien finibus. Duis nec libero convallis lectus pharetra blandit ut ac odio. Vivamus nec dui quis sem convallis pulvinar. Maecenas sodales sollicitudin leo a faucibus.",
            "dog brown lazy the over jumped fox red quick The");

    List<Double> groundTruthWERsCommonsTokenizer = List.of(
            8 / 12.,
            3 / 66.,
            1.0);

    // nlp tokenizer creates a different number of tokens and thus produces different answers
    List<Double> groundTruthWERsNLPTokenizer = List.of(
            9 / 14.,
            3 / 78.,
            1.0);

    // nlp whitespace tokenizer creates a different number of tokens and thus produces different answers
    List<Double> groundTruthsWhitespaceTokenizer = List.of(
            8 / 14.,
            3 / 78.,
            10 / 10.);

    @Test
    public void testCommonsTokenizer() {
        for (int i = 0; i < references.size(); i++) {
            WordErrorRate wer = new WordErrorRate();
            double werValue = wer.calculate(
                    new StringTokenizer(references.get(i)).getTokenList(),
                    new StringTokenizer(inputs.get(i)).getTokenList()).getWordErrorRate();

            assertEquals(groundTruthWERsCommonsTokenizer.get(i), werValue, 1e-5);
        }
    }

    @Test
    public void testDefaultWhitespaceTokenizer() {
        for (int i = 0; i < references.size(); i++) {
            WordErrorRateResult werr = new WordErrorRate().calculate(references.get(i), inputs.get(i));
            assertEquals(groundTruthsWhitespaceTokenizer.get(i), werr.getWordErrorRate(), 1e-5);
        }
    }

    @Test
    public void testProvidedOpenNLPTokenizer() {
        for (int i = 0; i < references.size(); i++) {
            WordErrorRateResult werr = new WordErrorRate(SimpleTokenizer.INSTANCE).calculate(references.get(i), inputs.get(i));
            assertEquals(groundTruthWERsNLPTokenizer.get(i), werr.getWordErrorRate(), 1e-5);
        }
    }

    @Test
    public void testResultObject() {
        WordErrorRateResult werr = new WordErrorRate(SimpleTokenizer.INSTANCE).calculate(references.get(0), inputs.get(0));
        assertEquals(7, werr.getAlignmentCounters().correct);
        assertEquals(1, werr.getAlignmentCounters().deletions);
        assertEquals(2, werr.getAlignmentCounters().insertions);
        assertEquals(6, werr.getAlignmentCounters().substitutions);
        assertEquals(
                "Word Error Rate: 0.6428571343421936\n" +
                        " Reference: *  | This  | is  | the  | test        | reference  | ,  | to    | which  | ***  | I        | will  | compare  | alignment  | against  | .  |\n" +
                        "Hypothesis: I  | '     | m   | a    | hypothesis  | reference  | ,  | from  | which  | the  | aligner  | will  | compare  | *********  | against  | .  |\n" +
                        "    Labels: I  | S     | S   | S    | S           | C          | C  | S     | C      | I    | S        | C     | C        | D          | C        | C  |\n" +
                        "TokenSequenceAlignmentCounters{substitutions=6, insertions=2, deletions=1, correct=7}",
                werr.toString());
    }

}
