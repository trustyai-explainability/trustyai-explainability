package org.kie.trustyai.service.payloads.metrics.drift.fouriermmd;

/**
 * Parameters for Fourier MMD Drift.
 */
public class FourierMMDParameters {

    private int nWindow = 168;
    private int nTest = 100;
    private int nMode = 512;
    private int randomSeed = 22;
    private double sig = 10.0;
    private boolean deltaStat = true;
    private double epsilon = 1.0e-7;

    public int getnWindow() {
        return nWindow;
    }

    public void setnWindow(int nWindow) {
        this.nWindow = nWindow;
    }

    public int getnTest() {
        return nTest;
    }

    public void setnTest(int nTest) {
        this.nTest = nTest;
    }

    public int getnMode() {
        return nMode;
    }

    public void setnMode(int nMode) {
        this.nMode = nMode;
    }

    public int getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed(int randomSeed) {
        this.randomSeed = randomSeed;
    }

    public double getSig() {
        return sig;
    }

    public void setSig(double sig) {
        this.sig = sig;
    }

    public boolean getDeltaStat() {
        return deltaStat;
    }

    public void setDeltaStat(boolean deltaStat) {
        this.deltaStat = deltaStat;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

}
