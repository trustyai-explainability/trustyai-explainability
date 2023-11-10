package org.kie.trustyai.metrics.language.utils;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.metrics.language.utils.tokenizers.CommonsTokenizer;

import opennlp.tools.tokenize.SimpleTokenizer;

import static org.junit.jupiter.api.Assertions.*;

class CommonsTokenizerTest {
    SimpleTokenizer simpleTokenizer = SimpleTokenizer.INSTANCE;
    CommonsTokenizer tokenizer = new CommonsTokenizer();

    @Test
    public void testTokenizer() {

        String input = "This is a test input";
        String[] output = { "This", "is", "a", "test", "input" };
        assertArrayEquals(output, tokenizer.tokenize(input));
    }

    @Test
    public void testSpanSingleDelim() {
        CommonsTokenizer tokenizer = new CommonsTokenizer();
        String input = "This is a test input";
        assertArrayEquals(simpleTokenizer.tokenizePos(input), tokenizer.tokenizePos(input));
    }

    @Test
    public void testSpanComplexDelim() {
        String input = "This    is a \n     test input    ";
        assertArrayEquals(simpleTokenizer.tokenizePos(input), tokenizer.tokenizePos(input));
    }

}
