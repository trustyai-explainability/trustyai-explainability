package org.kie.trustyai.metrics.language;

import opennlp.tools.tokenize.Tokenizer;
import org.kie.trustyai.metrics.language.utils.TokenizerUtils;

public class AbstractNLPPerformanceMetric {
    private final Tokenizer tokenizer;

    public AbstractNLPPerformanceMetric() {
        tokenizer = TokenizerUtils.getDefaultTokenizer();

    }

    public AbstractNLPPerformanceMetric(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }
}
