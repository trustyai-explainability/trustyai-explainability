package org.kie.trustyai.metrics.language.distance;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import java.util.List;
import java.util.stream.IntStream;

public class Levenshtein {

    private Levenshtein() {

    }

    /**
     * Calculate Levenshtein distances between two sentences, at the token level
     * @see <a href="https://en.wikipedia.org/wiki/Levenshtein_distance">https://en.wikipedia.org/wiki/Levenshtein_distance</a>
     * @param tokensA First list of tokens
     * @param tokensB Second list of tokens
     * @return A {@link LevenshteinResult} containing the distance and counters
     */
    public static LevenshteinResult calculateToken(List<String> tokensA, List<String> tokensB) {
        final int refSize = tokensA.size();
        final int hypSize = tokensB.size();
        final RealMatrix matrix = MatrixUtils.createRealMatrix(refSize + 1, hypSize + 1);

        // Initialize matrix
        IntStream.rangeClosed(0, refSize).forEach(n -> matrix.setEntry(n, 0, n));
        IntStream.rangeClosed(0, hypSize).forEach(n -> matrix.setEntry(0, n, n));

        // Compute Levenshtein matrix
        for (int refIndex = 1; refIndex <= refSize; refIndex++) {
            for (int hypIndex = 1; hypIndex <= hypSize; hypIndex++) {
                if (tokensA.get(refIndex - 1).equals(tokensB.get(hypIndex - 1))) {
                    matrix.setEntry(refIndex, hypIndex, matrix.getEntry(refIndex - 1, hypIndex - 1));
                } else {
                    double substituteCost = matrix.getEntry(refIndex - 1, hypIndex - 1) + 1;
                    double insertCost = matrix.getEntry(refIndex, hypIndex - 1) + 1;
                    double deleteCost = matrix.getEntry(refIndex - 1, hypIndex) + 1;
                    double min = Math.min(Math.min(substituteCost, insertCost), deleteCost);
                    matrix.setEntry(refIndex, hypIndex, min);
                }
            }
        }

        int correct = 0;
        int substitutions = 0;
        int insertions = 0;
        int deletions = 0;

        // Count operations
        int refIndex = refSize;
        int hypIndex = hypSize;
        while (refIndex > 0 || hypIndex > 0) {
            if (refIndex > 0 && hypIndex > 0 && tokensA.get(refIndex - 1).equals(tokensB.get(hypIndex - 1))) {
                // It's a match
                refIndex--;
                hypIndex--;
                correct++;
            } else if (refIndex > 0 && hypIndex > 0 && matrix.getEntry(refIndex, hypIndex) == matrix.getEntry(refIndex - 1, hypIndex - 1) + 1) {
                // It's a substitution
                refIndex--;
                hypIndex--;
                substitutions++;
            } else if (hypIndex > 0 && matrix.getEntry(refIndex, hypIndex) == matrix.getEntry(refIndex, hypIndex - 1) + 1) {
                // It's an insertion
                hypIndex--;
                insertions++;
            } else if (refIndex > 0 && matrix.getEntry(refIndex, hypIndex) == matrix.getEntry(refIndex - 1, hypIndex) + 1) {
                // It's a deletion
                refIndex--;
                deletions++;
            }
        }

        int distance = (int) matrix.getEntry(refSize, hypSize);
        final LevenshteinCounters counters = new LevenshteinCounters(substitutions, insertions, deletions, correct);
        return new LevenshteinResult(distance, counters);
    }
}
