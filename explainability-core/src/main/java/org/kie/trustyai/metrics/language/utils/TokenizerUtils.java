package org.kie.trustyai.metrics.language.utils;

import java.io.IOException;
import java.io.InputStream;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import opennlp.tools.tokenize.WhitespaceTokenizer;

public class TokenizerUtils {

    private TokenizerUtils() {

    }

    public static Tokenizer getDefaultTokenizer() {
        try (InputStream modelIn = TokenizerUtils.class.getResourceAsStream("/opennlp/models/en-token.bin")) {
            if (modelIn != null) {
                return new TokenizerME(new TokenizerModel(modelIn));
            } else {
                return WhitespaceTokenizer.INSTANCE;
            }
        } catch (IOException e) {
            return WhitespaceTokenizer.INSTANCE;
        }
    }
}
