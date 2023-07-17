package org.kie.trustyai.explainability.local.tssaliency;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.kie.trustyai.explainability.model.PredictionProvider;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;

public class TSSaliencyRunner implements Runnable {

    private RealMatrix x;
    private RealVector alphaArray;
    private RealMatrix baseValueMatrix;
    private RealMatrix score;
    private PredictionProvider model;
    private TSSaliencyExplainer explainer;
    private List<Integer> alphaList;

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

            // System.out.println("thread " + Thread.currentThread().getName() + "
            // started");

            long startTime = System.nanoTime();

            int T = x.getRowDimension();
            int F = x.getColumnDimension();

            for (Integer I : alphaList) {

                int i = I.intValue();
                double alpha = alphaArray.getEntry(i);

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

                // System.out.println(gDivNalpha.toString());

                synchronized (score) {
                    IntStream.range(0, T).forEach(
                        t -> score.setRowVector(t, score.getRowVector(t).add(gDivNalpha.getRowVector(t))));

                    // System.out.println(score.toString());
                }

            }

            long endTime = System.nanoTime();

            double processingms = (endTime - startTime) / 1000000.0;

            // String alphas = "";
            // for (Integer I : alphaList) {
            //     alphas += I.toString() + ",";
            // }

            // System.out.println("thread " + Thread.currentThread().getName() + " done " + alphas + " processing " + processingms);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
