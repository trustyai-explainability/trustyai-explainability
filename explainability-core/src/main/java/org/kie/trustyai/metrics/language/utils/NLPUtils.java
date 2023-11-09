package org.kie.trustyai.metrics.language.utils;

import java.util.Comparator;
import java.util.List;

public class NLPUtils {

    private NLPUtils() {

    }

    /**
     * Finds the reference length that is the closest to the hypothesis length.
     * In case of a tie (e.g. 4(r) < 5(h) < 6(r)), the smallest will be returned.
     * This is to reduce the brevity penalty for terser hypotheses.
     *
     * @param references       A list of lists of references.
     * @param hypothesisLength The length of the hypothesis.
     * @return The length of the reference that's closest to the hypothesis length.
     */
    public static int closestReferenceLength(List<List<String>> references, int hypothesisLength) {
        return references.stream()
                .mapToInt(List::size)
                .boxed()
                .min(Comparator.comparingInt(a -> Math.abs(a - hypothesisLength)))
                .orElse(Integer.MAX_VALUE);
    }
}
