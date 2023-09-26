package org.kie.trustyai.metrics.drift.fouriermmd;

/*
 * Return value from FourierMMD.execute()
 */

public class FourierMMDResult {
    // return {
    // "drift": flag,
    // "magnitude": magnitude,
    // "computed_values": computed_values,
    // }

    public boolean drifted;
    public double pValue;
    public double relativeMMDScore;

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("DriftResult{\n");
        buf.append("drifted = " + drifted + "\n");
        buf.append("pValue = " + pValue + "\n");
        buf.append("relativeMMDScore = " + relativeMMDScore + "\n");
        buf.append("}\n");

        return buf.toString();
    }
}
