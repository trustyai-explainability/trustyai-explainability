package org.kie.trustyai.metrics.drift.fouriermmd;

/*
 * Return value from FourierMMD.execute()
 */

public class DriftResult {
    // return {
    // "drift": flag,
    // "magnitude": magnitude,
    // "computed_values": computed_values,
    // }

    public boolean drift;
    public double magnitude;
    public double computedValuesScore;

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();

        buf.append("DriftResult{\n");
        buf.append("drift = " + drift + "\n");
        buf.append("magnitude = " + magnitude + "\n");
        buf.append("computedValuesScore = " + computedValuesScore + "\n");
        buf.append("}\n");

        return buf.toString();
    }
}
