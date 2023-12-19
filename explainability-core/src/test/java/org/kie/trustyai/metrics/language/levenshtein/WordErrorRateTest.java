package org.kie.trustyai.metrics.language.levenshtein;

import java.util.List;

import org.apache.commons.text.StringTokenizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import opennlp.tools.tokenize.SimpleTokenizer;

class WordErrorRateTest {

    List<Double> groundTruthWERsCommonsTokenizer = List.of(
            8 / 12.,
            3 / 66.,
            1.0,
            0.25);

    // nlp tokenizer creates a different number of tokens and thus produces different answers
    List<Double> groundTruthWERsNLPTokenizer = List.of(
            9 / 14.,
            3 / 78.,
            1.0,
            0.25);

    // nlp whitespace tokenizer creates a different number of tokens and thus produces different answers
    List<Double> groundTruthsWhitespaceTokenizer = List.of(
            8 / 14.,
            3 / 78.,
            10 / 10.,
            0.25);

    @Test
    public void testCommonsTokenizer() {
        for (int i = 0; i < LevenshteinCommonTest.references.size(); i++) {
            WordErrorRate wer = new WordErrorRate();
            double werValue = wer.calculate(
                    new StringTokenizer(LevenshteinCommonTest.references.get(i)).getTokenList(),
                    new StringTokenizer(LevenshteinCommonTest.inputs.get(i)).getTokenList()).getValue();

            assertEquals(groundTruthWERsCommonsTokenizer.get(i), werValue, 1e-5);
        }
    }

    @Test
    public void testDefaultWhitespaceTokenizer() {
        for (int i = 0; i < LevenshteinCommonTest.references.size(); i++) {
            ErrorRateResult werr = new WordErrorRate().calculate(LevenshteinCommonTest.references.get(i), LevenshteinCommonTest.inputs.get(i));
            assertEquals(groundTruthsWhitespaceTokenizer.get(i), werr.getValue(), 1e-5);
        }
    }

    @Test
    public void testProvidedOpenNLPTokenizer() {
        for (int i = 0; i < LevenshteinCommonTest.references.size(); i++) {
            ErrorRateResult werr = new WordErrorRate(SimpleTokenizer.INSTANCE).calculate(LevenshteinCommonTest.references.get(i), LevenshteinCommonTest.inputs.get(i));
            assertEquals(groundTruthWERsNLPTokenizer.get(i), werr.getValue(), 1e-5);
        }
    }

}
