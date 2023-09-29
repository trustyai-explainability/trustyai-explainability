package org.kie.trustyai.metrics.drift.kstest;

import java.util.Map;

public class ApproxKSFitting {
    Map<String, GKSketch> fitSketches;

    public ApproxKSFitting(Map<String, GKSketch> fitSketches) {
        this.fitSketches = fitSketches;
    }

    public Map<String, GKSketch> getfitSketches() {
        return fitSketches;
    }

    @Override
    public String toString() {
        return "ApproxKSFitting{" +
                "fitSketches=" + fitSketches +
                '}';
    }
}