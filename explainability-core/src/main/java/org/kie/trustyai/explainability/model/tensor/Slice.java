package org.kie.trustyai.explainability.model.tensor;

import java.util.OptionalInt;

public class Slice {
    final OptionalInt from;
    final OptionalInt to;
    final OptionalInt at;
    final SliceType sliceType;

    public enum SliceType {
        ALL,
        AT,
        FROM,
        TO,
        BETWEEN
    }

    private Slice(OptionalInt from, OptionalInt to, OptionalInt at, SliceType sliceType) {
        this.from = from;
        this.to = to;
        this.at = at;
        this.sliceType = sliceType;
    }

    // == FACTORY METHODS ==============================================================================================
    /**
     * Slice an axis on the range [a, b). This is equivalent to the Numpy arr[a:b] syntax.
     **/
    public static Slice between(int a, int b) {
        if (b <= a) {
            throw new IllegalArgumentException("Slice.between(a, b) requires that a < b, received a=" + a + ", b=" + b);
        }

        return new Slice(OptionalInt.of(a), OptionalInt.of(b), OptionalInt.empty(), SliceType.BETWEEN);
    }

    /**
     * Slice an axis on the range [from, ). This is equivalent to the Numpy arr[from:] syntax.
     **/
    public static Slice from(int from) {
        return new Slice(OptionalInt.of(from), OptionalInt.empty(), OptionalInt.empty(), SliceType.FROM);
    }

    /**
     * Slice an axis on the range [0, to). This is equivalent to the Numpy arr[:to] syntax.
     **/
    public static Slice to(int to) {
        return new Slice(OptionalInt.empty(), OptionalInt.of(to), OptionalInt.empty(), SliceType.TO);
    }

    /**
     * Slice the idx'th element from axis. This is equivalent to the Numpy arr[idx] syntax.
     **/
    public static Slice at(int idx) {
        return new Slice(OptionalInt.empty(), OptionalInt.empty(), OptionalInt.of(idx), SliceType.AT);
    }

    /**
     * Slice the entirety of an axis. This is equivalent to the Numpy arr[:] syntax.
     **/
    public static Slice all() {
        return new Slice(OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(), SliceType.ALL);
    }

    // == GETTERS ======================================================================================================
    public int size() {
        if (at.isPresent()) {
            return 1;
        } else {
            return to.getAsInt() - from.getAsInt();
        }
    }

    public SliceType getSliceType() {
        return sliceType;
    }

    // get the "at" index of this Slice
    public int at() {
        if (at.isPresent()) {
            return this.at.getAsInt();
        } else {
            throw new IllegalArgumentException("This slice is a " + sliceType + " type, and does not have an 'at' parameter");
        }
    }

    // get the "from" index of this Slice
    public int from() {
        if (from.isPresent()) {
            return this.from.getAsInt();
        } else {
            throw new IllegalArgumentException("This slice is a " + sliceType + " type, and does not have an 'from' parameter");
        }
    }

    // get the "to" index of this Slice
    public int to() {
        if (to.isPresent()) {
            return this.to.getAsInt();
        } else {
            throw new IllegalArgumentException("This slice is a " + sliceType + " type, and does not have an 'to' parameter");
        }
    }
}
