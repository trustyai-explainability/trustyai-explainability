package org.kie.trustyai.metrics.language.rouge;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.kie.trustyai.metrics.language.AbstractNLPPerformanceMetric;
import org.kie.trustyai.metrics.language.utils.NGramUtils;
import org.kie.trustyai.metrics.language.utils.SentenceUtils;
import org.kie.trustyai.metrics.language.utils.F1Score;

public class ROUGE extends AbstractNLPPerformanceMetric<Double, String>{

    public enum RougeTypes {
        ROUGE_LSUM,
        ROUGE1,
        ROUGE2,
        ROUGEL;

        public int getN() {
            switch (this) {
                case ROUGE1:
                    return 1;
                case ROUGE2:
                    return 2;
                default:
                    throw new UnsupportedOperationException("getN() is not supported for " + this);
            }
        }
    }

    private RougeTypes rougeType;

    public ROUGE(RougeTypes rougeType){
        super();
        this.rougeType = rougeType;
    }

    @Override
    public Double calculate(String reference, String hypothesis) {
        return calculate(reference, hypothesis, rougeType);
    }

    /**
     * Calculates the ROUGE score for a reference and a hypothesis.
     *
     * @param reference The reference string.
     * @param hypothesis The hypothesis string.
     * @param rougeType The ROUGE type string.
     * @return The ROUGE score.
     */
    public Double calculate(String reference, String hypothesis, RougeTypes rougeType) {
        switch (rougeType){
            case ROUGE_LSUM:{
                 // Split the reference and hypothesis into sentences
                final String[] referenceSentences = SentenceUtils.splitSentences(reference);
                final String[] hypothesisSentences = SentenceUtils.splitSentences(hypothesis);
                return rougeLsum(referenceSentences, hypothesisSentences);
            }
            case ROUGEL:
            case ROUGE1:
            case ROUGE2:
                // Remove punctuation from the reference and hypothesis
                reference = reference.replaceAll("\\p{Punct}", "");
                hypothesis = hypothesis.replaceAll("\\p{Punct}", "");
                // Tokenize the reference and hypothesis
                final List<String> referenceTokens =  Arrays.asList(this.getTokenizer().tokenize(reference));
                final List<String> hypothesisTokens = Arrays.asList(this.getTokenizer().tokenize(hypothesis));
                if (rougeType != rougeType.ROUGEL){
                    // Get the ROUGE-N type and calculate the n-grams in the reference and hypothesis accordingly
                    final int n = rougeType.getN();
                    final Map<String, Integer> referenceNgramCount = NGramUtils.countNgrams(referenceTokens, n);
                    final Map<String, Integer> hypothesisNGramCount = NGramUtils.countNgrams(hypothesisTokens, n);
                    return rougeN(referenceNgramCount, hypothesisNGramCount);
                }
                return rougeL(referenceTokens, hypothesisTokens);
            default:
                return 0.0;
        }
    }

     /**
     * Calculate ROUGE-N score given the reference and hypothesis n-gram count
     *
     * @param referenceNgramCount N-grams for reference
     * @param hypothesisNgramCount N-grams for hypothesis
     * @return ROUGE-N score
     */
    public Double rougeN(Map<String, Integer> referenceNgramCount,  Map<String, Integer> hypothesisNgramCount){
        // Initalize total match count between reference and hypothesis tokens
        int totalMatchCount = 0;

        // Get length of reference and hypothesis tokens
        int referenceTokensLength = referenceNgramCount.values().stream().mapToInt(Integer::intValue).sum();
        int hypothesisTokensLength = hypothesisNgramCount.values().stream().mapToInt(Integer::intValue).sum();;

        // Count matches between reference and hypothesis tokens
        for (Map.Entry<String, Integer> entry : hypothesisNgramCount.entrySet()){
            final String ngram = entry.getKey();
            final int count = entry.getValue();
            totalMatchCount += Math.min(count, referenceNgramCount.getOrDefault(ngram, 0));
        }

        // Calculate precision and recall scores
        final Double rawPrecisionScore = (double) totalMatchCount / referenceTokensLength;
        final Double rawRecallScore = (double) totalMatchCount / hypothesisTokensLength;

        return F1Score.calculate(rawPrecisionScore, rawRecallScore);
    }

    /**
     * Calculate ROUGE-L score given the reference and hypothesis tokens
     *
     * @param referenceTokens Reference tokens
     * @param hypothesisTokens Hypothesis tokens
     * @return ROUGE-L score
     */
    public Double rougeL(List<String> referenceTokens,  List<String> hypothesisTokens){
        // Initialize matrix to store the number of consecutive token matches between
        // the reference and hypothesis
        int rows = referenceTokens.size();
        int cols = hypothesisTokens.size();
        int[][] lcsTable = new int[rows + 1][cols + 1];

        // Iterate through each reference-hypothesis token pair and add 1 to the previous
        // value of the diagonal if they are equal
        for (int i=1; i <= rows; i++){
            for (int j=1; j <= cols; j++){
                if (referenceTokens.get(i - 1).equals(hypothesisTokens.get(j - 1))){
                    lcsTable[i][j] = lcsTable[i - 1][j - 1] + 1;
                }
                else {
                    lcsTable[i][j] = Math.max(lcsTable[i - 1][j], lcsTable[i][j - 1]);
                }
            }
        }
        // Get the total number of reference-hypothesis matches
        int lcsLength = lcsTable[rows][cols];
        double rawPrecisionScore = (double) lcsLength / cols;
        double rawRecallScore = (double) lcsLength / rows;
        return F1Score.calculate(rawPrecisionScore, rawRecallScore);
    }

    /**
     * Calculate ROUGE-Lsum score for a list of references and a list of hypotheses
     *
     * @param referenceSentences A list of reference sentences
     * @param hypothesisSentences A list of hypothesis sentences
     * @return The ROUGE-LSum score
     */
    public Double rougeLsum(String[] referenceSentences, String[] hypothesisSentences){
        // Initialize F1 score
        double fScore = 0;

        // Calculate and aggregate the ROUGE-L score for each reference-hypothesis sentence pair
        for (int i=0; i < referenceSentences.length; i++){
            final List<String> hypothesisTokens = Arrays.asList(this.getTokenizer().tokenize(hypothesisSentences[i]));
            final List<String> referenceTokens =  Arrays.asList(this.getTokenizer().tokenize(referenceSentences[i]));
            fScore += rougeL(referenceTokens, hypothesisTokens);
        }
        // Average the F1 score over the number of reference sentences
        return fScore / referenceSentences.length;
    }
}
