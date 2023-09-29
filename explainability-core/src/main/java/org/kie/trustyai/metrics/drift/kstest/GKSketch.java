package org.kie.trustyai.metrics.drift.kstest;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Triple;

// This class implements Greenwald-Khanna epsilon sketch for approximate quantiles as mentioned in Lall's.
// Algorithm is based on M. Greenwald and S. Khanna. Space-efficient online computation of quantile summaries. In SIGMOD, pages 58–66, 2001
public class GKSketch {
    private double epsilon;
    /*
     * initialize the summary structure for storing Triple(v,g,delta) tuples where
     * # v : is the value
     * # g : number of values covered by the tuple
     * # delta : r_max - r_min; // covered range
     */
    private List<Triple<Double, Integer, Integer>> summary;

    // track max and min values of sketch
    private double xmin;
    private double xmax;
    private int numx; //number of values we have seen so far

    public GKSketch(double epsilon, double xmin, double xmax, int d) {
        this.epsilon = epsilon;
        this.xmin = xmin;
        this.xmax = xmax;
        this.numx = d;
    }

    public GKSketch(double epsilon) {
        this.epsilon = epsilon;
        this.summary = new ArrayList<Triple<Double, Integer, Integer>>();
        this.numx = 0;
        this.xmin = Double.MIN_VALUE;
        this.xmax = Double.MAX_VALUE;
    }

    // Main method to process stream
    public void insert(double x) {
        int compressSteps = (int) Math.floor(1.0 / (2.0 * epsilon));
        if (numx % compressSteps == 0) {
            compress();
        }
        numx++;
        try {
            update(x);
        } catch (GKException e) {
            throw new RuntimeException("Unexpected execution of GKSketch:  " + e.getMessage());
        }
    }

    private void compress() {
        //Iterate right to left through summary
        if (summary.size() < 3) {
            return;
        }
        for (int i = summary.size() - 2; i > 1; i--) {
            int bandCurr = findBand(i);
            int bandNext = findBand(i + 1);
            boolean cond1 = bandCurr <= bandNext;
            List<Integer> children = getChildren(i); //Returns indexes of children at left
            int sumG = getSubtreeSumG(i, children); // Sum of covered items till ith
            Triple<Double, Integer, Integer> t = summary.get(i + 1);
            int lhs = sumG + (int) t.getMiddle() + (int) t.getRight();
            double rhs = 2 * epsilon * numx;
            boolean cond2 = lhs < rhs;
            if (cond1 && cond2) {
                Triple<Double, Integer, Integer> newT = Triple.of(t.getLeft(), t.getMiddle() + sumG, t.getRight());
                summary.remove(i + 1);
                summary.remove(i);
                int insertIndex = i;
                for (int j : children) {
                    summary.remove(j);
                    insertIndex = j;
                }
                summary.add(insertIndex, newT);
                i = insertIndex;
            }
        }
    }

    private int getSubtreeSumG(int i, List<Integer> children) {
        // sum of g values for subtree rooted at node i
        int gstar = summary.get(i).getMiddle();
        for (int child : children) {
            gstar = gstar + summary.get(child).getMiddle();
        }
        return gstar;
    }

    private List<Integer> getChildren(int i) {
        // entries in the summary that are children of Ith element
        List<Integer> subtree = new ArrayList<Integer>();
        int deltaI = summary.get(i).getRight();
        for (int j = i - 1; j >= 0; j--) {
            int deltaJ = summary.get(j).getRight();
            if (deltaJ > deltaI)
                subtree.add(j);
            else
                break;
        }
        return subtree;
    }

    private int findBand(int i) {
        int varp = (int) (2 * epsilon * numx);
        int delta = summary.get(i).getRight();
        int diff = varp - delta + 1;
        int band = (diff == 1) ? 0 : (int) (Math.log(diff) / Math.log(2));

        return band;
    }

    // Update the sketch summary with a new value
    private void update(double x) throws GKException {
        if (numx == 1) {
            xmin = x;
            xmax = x;
            summary.add(Triple.of(x, 1, 0));
        } else if (x <= xmin) { // add as new min
            xmin = x;
            summary.add(0, Triple.of(x, 1, 0));
        } else if (x >= xmax) { // add as new max
            xmax = x;
            summary.add(Triple.of(x, 1, 0));
        } else {
            int i = findIndex(x);
            int g_i = summary.get(i).getMiddle();
            int delta_i = summary.get(i).getRight();
            int delta = g_i + delta_i - 1;
            summary.add(i, Triple.of(x, 1, delta));
        }
    }

    private int findIndex(double x) throws GKException {
        // TODO: convert this to binary search
        // '''smallest i such that v[i-1]<= v < v[i]
        int newI = -1;
        int j = 0;
        for (int i = 1; i < summary.size(); i++) {
            j = i - 1;
            Triple<Double, Integer, Integer> tj = summary.get(j);
            double vj = (double) tj.getLeft();
            if (x >= vj) {
                Triple<Double, Integer, Integer> ti = summary.get(i);
                double vi = (double) ti.getLeft();
                if (x < vi) {
                    newI = i;
                    break;
                }
            }
        }
        if (newI < 0) {
            throw new GKException(String.format(
                    "Could not find insertion location for %d in GKsketch values in range [%d,%d]\".",
                    x, xmin, xmax));
        }

        return newI;
    }

    public int rank(double x) throws GKException {
        //find approx rank of x
        if (x <= xmin) {
            return 1;
        }
        if (x >= xmax) {
            return numx;
        }
        int index = findIndex(x);
        int rank = getMinRank(index) - 1;

        return rank;
    }

    private int getMinRank(int index) {
        if (index == 0)
            return 1;
        int rmin = 0;
        for (int j = 0; j < index + 1; j++) {
            rmin = rmin + summary.get(j).getMiddle();
        }
        return rmin;
    }

    private int getMaxRank(int index, int rmin) {
        int delta = summary.get(index).getRight();
        int rmax = delta + rmin;
        return rmax;
    }

    public double quantile(double phi) {
        // '''Estimate a quantile for a given probability'''
        if (phi < 0 || phi > 1.0) {
            throw new IllegalArgumentException("quantile must be between 0 and 1");
        }
        int targetRank = (int) Math.ceil(phi * numx);
        double rhs = epsilon * numx;
        if (targetRank >= numx) {
            return xmax;
        }
        double val = xmax;
        for (int i = 0; i < summary.size(); i++) {
            int minRank = getMinRank(i);
            int maxRank = getMaxRank(i, minRank);
            if (((targetRank - minRank <= rhs)) && ((maxRank - targetRank <= rhs))) {
                val = summary.get(i).getLeft();
                break;
            }
        }
        return val;

    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public double getXmax() {
        return xmax;
    }

    public int getNumx() {
        return numx;
    }

    public double getXmin() {
        return xmin;
    }

    public int size() {
        // Returns sketch summary size
        return summary.size();
    }

    public List<Triple<Double, Integer, Integer>> getSummary() {
        return summary;
    }

    public void setSummary(List<Triple<Double, Integer, Integer>> summary) {
        this.summary = summary;
    }

    public List<Double> approxQuantiles(double[] probs) {
        List<Double> quantiles = new ArrayList<>(probs.length);
        for (double phi : probs) {
            double q = quantile(phi);
            quantiles.add(q);
        }
        return quantiles;

    }

    /**
     * Exception from unexpected execution of GKSketch.
     *
     */
    public static class GKException extends Exception {
        public GKException(String message) {
            super(message);
        }
    }

    @Override
    public String toString() {
        return "GKSketch [summary=" + summary + ", xmin=" + xmin + ", xmax=" + xmax + ", numx=" + numx + "]";
    }

}
