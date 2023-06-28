package org.kie.trustyai.explainability.local.tssaliency;

import java.util.List;

import org.kie.trustyai.explainability.model.PredictionProvider;

public class TSSaliencyRunner implements Runnable {

    private double[][] x;
    private double[] alphaArray;
    private double[] baseValue;
    private double[][] score;
    private PredictionProvider model;
    private TSSaliencyExplainer explainer;
    private List<Integer> alphaList;

    public TSSaliencyRunner(double[][] x, double[] alphaArray, double[] baseValue, double[][] score,
            PredictionProvider model,
            TSSaliencyExplainer explainer, List<Integer> alphaList) {
        this.x = x;
        this.alphaArray = alphaArray;
        this.baseValue = baseValue;
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

            int T = x.length;
            int F = x[0].length;

            for (Integer I : alphaList) {

                int i = I.intValue();
                double alpha = alphaArray[i];

                // s = alpha(i) * X + (1 - alpha(i)) * (1(T) * transpose(b))

                double[][] s = new double[T][F];
                for (int t = 0; t < T; t++) {

                    for (int f = 0; f < F; f++) {
                        s[t][f] = alpha * x[t][f] + (1.0 - alpha) * baseValue[f];
                    }
                }

                // Compute Monte Carlo gradient (per time and feature dimension):

                // g = MC_GRADIENT(s; f; ng)
                double[][] g = explainer.monteCarloGradient(s, model);

                // Update Score:

                synchronized (score) {
                    for (int t = 0; t < T; t++) {
                        for (int f = 0; f < F; f++) {
                            score[t][f] = score[t][f] + g[t][f] / explainer.nalpha;
                        }
                    }
                }

            }

            long endTime = System.nanoTime();

            double processingms = (endTime - startTime) / 1000000.0;

            String alphas = "";
            for (Integer I : alphaList) {
                alphas += I.toString() + ",";
            }

            System.out.println(
                    "thread " + Thread.currentThread().getName() + " done " + alphas + " process " + processingms);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
