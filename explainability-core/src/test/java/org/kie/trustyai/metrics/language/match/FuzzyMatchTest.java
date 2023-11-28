package org.kie.trustyai.metrics.language.match;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.metrics.language.utils.tokenizers.TokenizerUtils;

import opennlp.tools.tokenize.Tokenizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuzzyMatchTest {
    List<String> references = List.of(
            "This is the test reference, to which I will compare alignment against.",
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur condimentum velit id velit posuere dictum. Fusce euismod tortor massa, nec euismod sapien laoreet non. Donec vulputate mi velit, eu ultricies nibh iaculis vel. Aenean posuere urna nec sapien consectetur, vitae porttitor sapien finibus. Duis nec libero convallis lectus pharetra blandit ut ac odio. Vivamus nec dui quis sem convallis pulvinar. Maecenas sodales sollicitudin leo a faucibus.",
            "The quick red fox jumped over the lazy brown dog");

    List<String> inputs = List.of(
            "I'm a hypothesis reference, from which the aligner  will compare against.",
            "Lorem ipsum sit amet, consectetur adipiscing elit. Curabitur condimentum velit id velit posuere dictum. Fusce blandit euismod tortor massa, nec euismod sapien blandit laoreet non. Donec vulputate mi velit, eu ultricies nibh iaculis vel. Aenean posuere urna nec sapien consectetur, vitae porttitor sapien finibus. Duis nec libero convallis lectus pharetra blandit ut ac odio. Vivamus nec dui quis sem convallis pulvinar. Maecenas sodales sollicitudin leo a faucibus.",
            "The quick red fox jumped over the lazy brown dog");

    Tokenizer commonsTokenizer = TokenizerUtils.getCommonsTokenizer();

    @Test
    void testExactMatch() {
        List<Boolean> expected = List.of(false, false, true);

        for (int i = 0; i < references.size(); i++) {
            assertEquals(expected.get(i), new FuzzyMatch().calculate(references.get(i), inputs.get(i)));
        }
    }

    @Test
    void testWERMatchNLPTokenizer() {
        List<Boolean> expected = List.of(true, true, true);

        for (int i = 0; i < references.size(); i++) {
            assertEquals(expected.get(i), new FuzzyMatch().calculateWER(references.get(i), inputs.get(i), 0.65));
        }
    }

    @Test
    void testWERMatchCommonsTokenizer() {
        // commons tokenizer has a higher WER for the first test case, above thresh
        List<Boolean> expected = List.of(false, true, true);

        for (int i = 0; i < references.size(); i++) {
            assertEquals(expected.get(i), new FuzzyMatch(commonsTokenizer).calculateWER(references.get(i), inputs.get(i), 0.65));
        }
    }

    @Test
    void testBLEUMatchDefaulTokenizer() {
        // commons tokenizer has a higher WER for the first test case, above thresh
        List<Boolean> expected = List.of(false, true, true);

        for (int i = 0; i < references.size(); i++) {
            assertEquals(expected.get(i), new FuzzyMatch().calculateBLEU(List.of(references.get(i)), inputs.get(i), 0.8));
        }
    }

    @Test
    void testBLEUMatchCommonsTokenizer() {
        // commons tokenizer has a higher WER for the first test case, above thresh
        List<Boolean> expected = List.of(false, true, true);

        for (int i = 0; i < references.size(); i++) {
            assertEquals(expected.get(i), new FuzzyMatch(commonsTokenizer).calculateBLEU(List.of(references.get(i)), inputs.get(i), 0.8));
        }
    }

}
