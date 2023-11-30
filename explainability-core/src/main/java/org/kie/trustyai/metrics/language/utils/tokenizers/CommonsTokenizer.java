package org.kie.trustyai.metrics.language.utils.tokenizers;

import org.apache.commons.text.StringTokenizer;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.Span;

public class CommonsTokenizer implements Tokenizer {
    /*
     * An implementation of the Apache Commons Text StringTokenizer as per the OpenNLP Tokenizer interface
     */

    @Override
    public String[] tokenize(String s) {
        return new StringTokenizer(s).getTokenArray();
    }

    @Override
    /**
     * Produce an array of spans, marking the original string indices of the first and last character of each token
     */
    public Span[] tokenizePos(String s) {
        String[] tokens = this.tokenize(s);
        int nTokens = tokens.length;
        Span[] outputSpans = new Span[nTokens];
        int strIdx = 0;
        String substr;
        for (int i = 0; i < nTokens; i++) {
            int tokenLength = tokens[i].length();
            outputSpans[i] = new Span(strIdx, strIdx + tokenLength);

            // if there is another token remaining
            if (i < nTokens - 1) {
                // add the current token length to the string index
                strIdx += tokenLength;

                // cut the substring to only include tokens we have not yet considered
                // this means the very next non-delimiter should be the next token
                substr = s.substring(strIdx);

                // find where the next token starts (i.e., add the size of the delimiter)
                strIdx += substr.indexOf(tokens[i + 1]);
                ;
            }
        }
        return outputSpans;
    }
}
