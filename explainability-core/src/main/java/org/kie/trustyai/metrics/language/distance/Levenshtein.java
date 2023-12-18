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



        int distance = (int) matrix.getEntry(refSize, hypSize);
        final LevenshteinCounters counters = getCounters(tokensA, tokensB, matrix);
        return new LevenshteinResult(distance, counters);
    }

    /**
     * Backtracks the Levenshtein distance matrix, as populated by the Wagner-Fischer algorithm,
     * to determine the specific edit operations. This method is based on "Algorithm Y" from
     * Wagner, Robert A.; Fischer, Michael J. (1974). "The String-to-String Correction Problem". Journal of the ACM. 21 (1): 168–173. doi:10.1145/321796.321811. S2CID 13381535.
     *
     * @see <a href="https://en.wikipedia.org/wiki/Wagner%E2%80%93Fischer_algorithm">https://en.wikipedia.org/wiki/Wagner%E2%80%93Fischer_algorithm</a>
     * @param tokensA The first list of tokens.
     * @param tokensB The second list of tokens.
     * @param D The populated Levenshtein distance matrix based on the Wagner-Fischer algorithm.
     * @return A {@link LevenshteinResult} object containing the edit operations and their counts.
     */
    public static LevenshteinCounters getCounters(List<String> tokensA, List<String> tokensB, RealMatrix D) {
        int i = tokensA.size();
        int j = tokensB.size();

        int correct = 0;
        int substitutions = 0;
        int insertions = 0;
        int deletions = 0;

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && tokensA.get(i - 1).equals(tokensB.get(j - 1))) {
                // No operation needed, it's a match
                i--;
                j--;
                correct++;
            } else if (i > 0 && j > 0 && D.getEntry(i, j) == D.getEntry(i - 1, j - 1) + 1) {
                // It's a substitution
                i--;
                j--;
                substitutions++;
            } else if (j > 0 && D.getEntry(i, j) == D.getEntry(i, j - 1) + 1) {
                // It's an insertion
                j--;
                insertions++;
            } else if (i > 0 && D.getEntry(i, j) == D.getEntry(i - 1, j) + 1) {
                // It's a deletion
                i--;
                deletions++;
            }
        }

        return new LevenshteinCounters(substitutions, insertions, deletions, correct);
}
}
