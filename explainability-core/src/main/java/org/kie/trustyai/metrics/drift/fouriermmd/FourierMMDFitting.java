package org.kie.trustyai.metrics.drift.fouriermmd;

import java.util.Arrays;

import org.kie.trustyai.explainability.model.Dataframe;

public class FourierMMDFitting {
    public Dataframe scale;
    public double[] aRef;
    public double mean_mmd;
    public double std_mmd;
    public int randomSeed;

    public FourierMMDFitting() {

    }

    public FourierMMDFitting(Dataframe scale, double[] aRef, double mean_mmd, double std_mmd, int random_seed) {
        this.scale = scale.copy();
        this.aRef = Arrays.copyOf(aRef, aRef.length);
        this.mean_mmd = mean_mmd;
        this.std_mmd = std_mmd;
        this.randomSeed = random_seed;
    }

    @Override
    public String toString() {
        return "FourierMMDFitting{" +
                "scale=" + this.scale.toString() + "," +
                "aRef=" + this.aRef.toString() + "," +
                "mean_mmd=" + this.mean_mmd + "," +
                "std_mmd=" + this.std_mmd +
                "randomSeed=" + this.randomSeed +
                "}";
    }
}
