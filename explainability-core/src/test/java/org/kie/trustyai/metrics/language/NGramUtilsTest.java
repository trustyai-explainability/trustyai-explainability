package org.kie.trustyai.metrics.language;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.metrics.language.utils.NGramUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NGramUtilsTest {

    @Test
    @DisplayName("Bigram generation")
    void bigramGeneration() {
        final String[] tokens = { "this", "is", "a", "test" };
        final List<String> bigrams = NGramUtils.generateNgrams(tokens, 2);

        assertEquals(3, bigrams.size());
        assertTrue(bigrams.contains("this is"));
        assertTrue(bigrams.contains("is a"));
        assertTrue(bigrams.contains("a test"));
    }

    @Test
    @DisplayName("Trigram generation")
    void trigramGeneration() {
        final String[] tokens = { "this", "is", "a", "test" };
        final List<String> trigrams = NGramUtils.generateNgrams(tokens, 3);

        assertEquals(2, trigrams.size());
        assertTrue(trigrams.contains("this is a"));
        assertTrue(trigrams.contains("is a test"));
    }

    @Test
    @DisplayName("Unigram generation")
    void unigramGeneration() {
        final String[] tokens = { "unigram" };
        final List<String> unigrams = NGramUtils.generateNgrams(tokens, 1);

        assertEquals(1, unigrams.size());
        assertTrue(unigrams.contains("unigram"));
    }

    @Test
    @DisplayName("Empty tokens")
    void emptyTokenArray() {
        final String[] tokens = {};
        final List<String> ngrams = NGramUtils.generateNgrams(tokens, 2);

        assertTrue(ngrams.isEmpty());
    }

    @Test
    @DisplayName("ngram size larger than tokens")
    void ngramsWithSizeLargerThanTokens() {
        final String[] tokens = { "short", "array" };
        final List<String> ngrams = NGramUtils.generateNgrams(tokens, 3);

        assertTrue(ngrams.isEmpty());
    }
}
