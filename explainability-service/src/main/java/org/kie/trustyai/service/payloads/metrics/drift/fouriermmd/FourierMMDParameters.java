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

    public int getnWindow() {
        return nWindow;
    }

    public void setnWindow() {
        this.nWindow = nWindow;
    }

    public int getnTest() {
        return nTest;
    }

    public void setnTest() {
        this.nTest = nTest;
    }

    public int getnMode() {
        return nMode;
    }

    public void setnMode() {
        this.nMode = nMode;
    }

    public int getRandomSeed() {
        return randomSeed;
    }

    public void setRandomSeed() {
        this.randomSeed = randomSeed;
    }

    public double getSig() {
        return sig;
    }

    public void setSig() {
        this.sig = sig;
    }

    public boolean getDeltaStat() {
        return deltaStat;
    }

    public void setDeltaStat() {
        this.deltaStat = deltaStat;
    }

}
