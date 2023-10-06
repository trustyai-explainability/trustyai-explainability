package org.kie.trustyai.metrics.drift.fouriermmd;

public class FourierMMDFitting {
    private int randomSeed;
    private boolean deltaStat;
    private int nMode;
    private double[] scale;
    private double[] aRef;
    private double meanMMD;
    private double stdMMD;

    public FourierMMDFitting() {
    };

    public FourierMMDFitting(int randomSeed, boolean deltaStat, int n_mode) {
        this.randomSeed = randomSeed;
        this.deltaStat = deltaStat;
        this.nMode = n_mode;
    }

    public int getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(int randomSeed) {
        this.randomSeed = randomSeed;
    }

    public boolean isDeltaStat() {
        return deltaStat;
    }

    public void setDeltaStat(boolean deltaStat) {
        this.deltaStat = deltaStat;
    }

    public int getnMode() {
        return nMode;
    }

    public void setnMode(int nMode) {
        this.nMode = nMode;
    }

    public double[] getScale() {
        return scale;
    }

    public void setScale(double[] scale) {
        this.scale = scale;
    }

    public double[] getaRef() {
        return aRef;
    }

    public void setaRef(double[] aRef) {
        this.aRef = aRef;
    }

    public double getMeanMMD() {
        return meanMMD;
    }

    public void setMeanMMD(double meanMMD) {
        this.meanMMD = meanMMD;
    }

    public double getStdMMD() {
        return stdMMD;
    }

    public void setStdMMD(double stdMMD) {
        this.stdMMD = stdMMD;
    }

    @Override
    public String toString() {
        return "FourierMMDFitting{" +
                "randomSeed=" + randomSeed +
                ", deltaStat=" + deltaStat +
                ", n_mode=" + nMode +
                '}';
    }
}
