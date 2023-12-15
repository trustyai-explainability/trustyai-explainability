package org.kie.trustyai.metrics.language.distance;

public class LevenshteinCounters {
    public int getSubstitutions() {
        return substitutions;
    }

    public int getInsertions() {
        return insertions;
    }

    public int getDeletions() {
        return deletions;
    }

    public int getCorrect() {
        return correct;
    }

    private final int substitutions;
    private final int insertions;
    private final int deletions;
    private final int correct;

    public LevenshteinCounters(int substitutions,
                               int insertions,
                               int deletions,
                               int correct) {
        this.substitutions = substitutions;
        this.insertions = insertions;
        this.deletions = deletions;
        this.correct = correct;
    }

    @Override
    public String toString() {
        return "LevenshteinCounters{" +
                "substitutions=" + substitutions +
                ", insertions=" + insertions +
                ", deletions=" + deletions +
                ", correct=" + correct +
                '}';
    }
}
