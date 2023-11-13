package org.kie.trustyai.metrics.language.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import opennlp.tools.ngram.NGramModel;
import opennlp.tools.util.StringList;

public class NGramUtils {

    private NGramUtils() {

    }

    public static List<String> generateNgrams(String[] tokens, int n) {
        final NGramModel nGramModel = new NGramModel();
        // Add n-grams to the model
        for (int i = 0; i <= tokens.length - n; i++) {
            final String[] ngramTokens = new String[n];
            System.arraycopy(tokens, i, ngramTokens, 0, n);
            final StringList ngram = new StringList(ngramTokens);
            nGramModel.add(ngram);
        }

        final List<String> ngrams = new ArrayList<>();
        for (StringList ngram : nGramModel) {
            ngrams.add(String.join(" ", ngram));
        }
        return ngrams;
    }

    public static Map<String, Integer> countNgrams(List<String> ngrams) {
        final Map<String, Integer> ngramCounts = new HashMap<>();
        for (String ngram : ngrams) {
            ngramCounts.put(ngram, ngramCounts.getOrDefault(ngram, 0) + 1);
        }
        return ngramCounts;
    }

    /**
     * Generate n-gram counts for a list of words
     * 
     * @param words
     * @param n
     * @return
     */
    public static Map<String, Integer> countNgrams(List<String> words, int n) {
        final Map<String, Integer> counts = new HashMap<>();
        for (int i = 0; i <= words.size() - n; i++) {
            final StringBuilder sb = new StringBuilder();
            for (int j = 0; j < n; j++) {
                if (j > 0)
                    sb.append(" ");
                sb.append(words.get(i + j));
            }
            final String ngram = sb.toString();
            counts.put(ngram, counts.getOrDefault(ngram, 0) + 1);
        }
        return counts;
    }
}
