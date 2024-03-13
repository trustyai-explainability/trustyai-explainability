package org.kie.trustyai.metrics.language.rouge;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.metrics.language.rouge.ROUGE.RougeTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validation tests based on Google's ROUGE tests
 */
class ROUGETest {

    @Test
    @DisplayName("Test ROUGE1")
    void validationROUGE1(){
        final ROUGE rougeCalculator = new ROUGE(RougeTypes.ROUGE1);
        final double score = rougeCalculator.calculate("testing one two", "testing");
        assertEquals(0.5, score);
    }

    @Test
    @DisplayName("Test ROUGE scores for zero matches")
    void validationEmpty(){
        final List<RougeTypes> rougeTypes = List.of(RougeTypes.ROUGE1, RougeTypes.ROUGE2, RougeTypes.ROUGEL, RougeTypes.ROUGE_LSUM);
        for (RougeTypes rougeType: rougeTypes){
            final ROUGE rougeCalculator = new ROUGE(rougeType);
            final double score = rougeCalculator.calculate("testing one two", "");
            assertEquals(0, score);
        }
    }

    @Test
    @DisplayName("Test ROUGE2")
    void validationROUGE2(){
        final ROUGE rougeCalculator = new ROUGE(RougeTypes.ROUGE2);
        final double score = rougeCalculator.calculate("testing one two", "testing one");
        assertEquals(0.66, score, 0.05);
    }

    @Test
    @DisplayName("Test ROUGEL for consecutive sentences")
    void validationROUGELConsecutive(){
        final ROUGE rougeCalculator = new ROUGE(RougeTypes.ROUGEL);
        final double score = rougeCalculator.calculate("testing one two", "testing one");
        assertEquals(0.8, score, 0.05);
    }

    @Test
    @DisplayName("Test ROUGEL for nonconsecutive sentences")
    void validationROUGELNonConsecutive(){
        final ROUGE rougeCalculator = new ROUGE(RougeTypes.ROUGEL);
        final double score = rougeCalculator.calculate("testing one two", "testing two");
        assertEquals(0.8, score, 0.05);
    }

    @Test
    @DisplayName("Test ROUGELSum")
    void validationROUGELSum(){
        final ROUGE rougeCalculator = new ROUGE(RougeTypes.ROUGE_LSUM);
        final double score = rougeCalculator.calculate("w1 w2 w3 w4 w5", "w1 w2 w6 w7 w8\nw1 w3 w8 w9 w5");
        assertEquals(0.5, score, 0.05);
    }

    @Test
    @DisplayName("Test ROUGELSum for non-word hypothesis")
    void validationROUGELSumNonWord(){
        final ROUGE rougeCalculator = new ROUGE(RougeTypes.ROUGE_LSUM);
        final double score = rougeCalculator.calculate("w1 w2 w3 w4 w5", "/");
        assertEquals(0, score);
    }
}
