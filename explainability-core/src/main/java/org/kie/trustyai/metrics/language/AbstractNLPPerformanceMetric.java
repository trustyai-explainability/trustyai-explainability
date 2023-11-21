package org.kie.trustyai.metrics.language;

import org.kie.trustyai.metrics.language.utils.TokenizerUtils;

import opennlp.tools.tokenize.Tokenizer;

public abstract class AbstractNLPPerformanceMetric<T, R> implements NLPPerformanceMetric<T, R> {
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
