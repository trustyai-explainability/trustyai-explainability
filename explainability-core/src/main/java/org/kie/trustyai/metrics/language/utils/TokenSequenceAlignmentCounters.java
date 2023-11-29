package org.kie.trustyai.metrics.language.utils;

public class TokenSequenceAlignmentCounters {
    public int substitutions;
    public int insertions;
    public int deletions;
    public int correct;

    @Override
    public String toString() {
        return "TokenSequenceAlignmentCounters{" +
                "substitutions=" + substitutions +
                ", insertions=" + insertions +
                ", deletions=" + deletions +
                ", correct=" + correct +
                '}';
    }
}
