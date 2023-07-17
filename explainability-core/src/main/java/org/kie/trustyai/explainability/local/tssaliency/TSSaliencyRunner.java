package org.kie.trustyai.explainability.local.tssaliency;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.kie.trustyai.explainability.model.PredictionProvider;

public class TSSaliencyRunner implements Runnable {

    final private RealMatrix x;
    final private RealVector alphaArray;
    final private RealMatrix baseValueMatrix;
    final private RealMatrix score;
    final private PredictionProvider model;
    final private TSSaliencyExplainer explainer;
    final private List<Integer> alphaList;

    public TSSaliencyRunner(RealMatrix x, RealVector alphaArray, RealMatrix baseValueMatrix, RealMatrix score,
            PredictionProvider model,
            TSSaliencyExplainer explainer, List<Integer> alphaList) {
        this.x = x;
        this.alphaArray = alphaArray;
        this.baseValueMatrix = baseValueMatrix;
        this.score = score;
        this.model = model;
        this.explainer = explainer;
        this.alphaList = alphaList;
    }

    public void run() {

        try {

            final int T = x.getRowDimension();
            
            for (Integer I : alphaList) {

                final int i = I.intValue();
                final double alpha = alphaArray.getEntry(i);

                // Compute affine sample:
                // s = alpha(i) * X + (1 - alpha(i)) * (1(T) * transpose(b))

                final RealMatrix alphaX = x.scalarMultiply(alpha); // Multiply each element of x by alpha[i]

                // Multiply each element of baseValue by (1 - alpha[i])
                final RealMatrix oneMinusAlphaBaseValue = baseValueMatrix.scalarMultiply(1.0 - alpha);
                final RealMatrix s = alphaX.add(oneMinusAlphaBaseValue); // Add the results together

                // Compute Monte Carlo gradient (per time and feature dimension):

                // g = MC_GRADIENT(s; f; ng)
                // g is also an array of doubles, where each element is a timepoint
                final RealMatrix g = explainer.monteCarloGradient(s, model);

                // Update Score:

                final RealMatrix gDivNalpha = g.scalarMultiply(1.0 / explainer.nalpha); // Divide g by nalpha

                synchronized (score) {
                    IntStream.range(0, T).forEach(
                        t -> score.setRowVector(t, score.getRowVector(t).add(gDivNalpha.getRowVector(t))));
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
    }
}
