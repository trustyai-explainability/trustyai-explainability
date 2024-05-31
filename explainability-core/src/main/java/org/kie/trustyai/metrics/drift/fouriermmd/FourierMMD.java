package org.kie.trustyai.metrics.drift.fouriermmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.utils.DataUtils;
import org.kie.trustyai.metrics.drift.HypothesisTestResult;

/**
 * FourierMMD is a class to identify drift in input data using a random Fourier
 * approximation to a Radial-Basis-Function Kernel Maximum Mean Discrepancy.
 *
 * References:
 * .. [#0] `Ji Zhao, Deyu Meng, 'FastMMD: Ensemble of Circular Discrepancy for
 * Efficient Two-Sample Test'
 * <https://arxiv.org/abs/1405.2664>`_
 * .. [#1] `Olivier Goudet, et al. 'Learning Functional Causal Models with
 * Generative Neural Networks'
 * <https://arxiv.org/abs/1709.05321>`_
 * 
 */

public class FourierMMD {

    private NormalDistribution normalDistribution = new NormalDistribution();
    private FourierMMDFitting fitStats;

    public FourierMMD(Dataframe dfTrain, boolean deltaStat, int n_test, int n_window, double sig, int randomSeed,
            int n_mode, double epsilon) {
        fitStats = precompute(dfTrain, deltaStat, n_test, n_window, sig, randomSeed, n_mode, epsilon);
    }

    public FourierMMD(FourierMMDFitting fourierMMDFitting) {
        fitStats = fourierMMDFitting;
    }

    /**
     * precompute() is used to train the FourierMMD drift model.
     * 
     * @param data the training data
     * @param deltaStat if 'true' , compute MMD score for Dx = x[t+1]-x[t],
     *        if 'false', compute MMD score for the input data, x
     * @param n_test number of MMD scores to compute the stastistics for MMD
     *        scores
     *        => MMD computation is repeated "n_test" times,
     *        each time "n_window" data is used to compute a MMD score
     * @param n_window number of the samples to compute a MMD score. "n_window"
     *        consecutive data.
     * @param sig sigma, a scale parameter of the kernel
     * @param randomSeed the seed for the random number generator
     * @param n_mode number of Fourier modes to approximate the original kernel.
     * @param epsilon minimum value for standard deviation
     * 
     * @return FourierMMDFitting the Fourier MMD fitting data.
     */

    // def learn(self, data: pd.DataFrame):
    public static FourierMMDFitting precompute(Dataframe data, boolean deltaStat, int n_test, int n_window, double sig,
            int randomSeed, int n_mode, double epsilon) {

        FourierMMDFitting computedStats = new FourierMMDFitting(randomSeed, deltaStat, n_mode);
        final Dataframe numericData = data.getNumericColumns();
        final List<String> columns = numericData.getRawColumnNames();
        final int numColumns = columns.size();

        final Dataframe xIn = deltaStat ? delta(numericData, numColumns) : numericData;
        final int numRows = xIn.getRowDimension();

        if (numRows == 0) {
            throw new IllegalArgumentException("Dataframe is empty");
        }

        final double[] scaleArray = Arrays.stream(xIn.std()).mapToDouble(d -> Math.max(d, epsilon) * sig).toArray();

        computedStats.setScale(scaleArray);

        // Init the RNG to the same seed that will be used for the execute() method
        // waveNum (theta) and bias (b) must be the same for precompute() and execute()
        final Random rg = new Random(randomSeed);
        final double[][] waveNum = getWaveNum(numColumns, rg, n_mode);
        final double[][] bias = getBias(rg, n_mode);
        final int ndata = Math.min(n_window * n_test, numRows);
        if (ndata <= n_window) {
            throw new IllegalArgumentException("n_window must be less than " + ndata);
        }

        // sample a random set of rows
        List<Integer> idxs = IntStream.range(0, numRows).boxed().collect(Collectors.toList());
        Collections.shuffle(idxs);
        idxs = idxs.subList(0, ndata);

        // grab those rows
        final List<List<Value>> x1 = idxs.stream().map(xIn::getRow).collect(Collectors.toList());
        final double[][] x1Scaled = getXScaled(numColumns, ndata, x1, scaleArray);

        // # 3. compute random Fourier mode
        double[] aRef = randomFourierCoefficients(x1Scaled, waveNum, bias, ndata, n_mode);
        computedStats.setaRef(aRef);

        // # 4. compute reference mmd score
        // sample_mmd = []
        final double[] sampleMMD = new double[n_test];

        // for i in range(n_test):
        for (int i = 0; i < n_test; i++) {
            // idx = np.random.randint(x_in.shape[0] - window_size)
            final int indexSize = ndata - n_window;
            final int idx = (int) (rg.nextDouble() * indexSize);

            // x1 = x_in[idx : idx + window_size]
            final double[][] xWindowed = new double[n_window][numColumns];
            for (int r = idx; r < (idx + n_window); r++) {
                for (int c = 0; c < numColumns; c++) {
                    xWindowed[r - idx][c] = xIn.getRow(r).get(c).asNumber();
                }
            }

            // x1 = x1 / self.learned_params["scale"].repeat(x1.shape[0], 0)
            for (int r = 0; r < n_window; r++) {
                for (int c = 0; c < numColumns; c++) {
                    xWindowed[r][c] /= scaleArray[c];
                }
            }

            final double[] aComp = randomFourierCoefficients(xWindowed, waveNum, bias, n_window, n_mode);
            // mmd = ((a_ref - a_comp) ** 2).sum()
            double mmd = 0.0;
            for (int c = 0; c < n_mode; c++) {
                final double dist = aRef[c] - aComp[c];
                final double term = dist * dist;
                mmd += term;
            }

            // sample_mmd += [mmd]
            sampleMMD[i] = mmd;
        }

        // self.learned_params["mean_mmd"] = np.nanmean(np.array(sample_mmd))
        final List<Double> sampleMMD2 = new ArrayList<Double>(n_test);
        for (int i = 0; i < n_test; i++) {
            if (!Double.isNaN(sampleMMD[i])) {
                sampleMMD2.add(sampleMMD[i]);
            }
        }

        if (sampleMMD2.size() == 0) {
            throw new IllegalArgumentException("sampleMMD2 length is zero");
        }

        final double[] sampleMMD2NoNaN = new double[sampleMMD2.size()];
        for (int i = 0; i < sampleMMD2.size(); i++) {
            sampleMMD2NoNaN[i] = sampleMMD2.get(i);
        }

        computedStats.setMeanMMD(DataUtils.getMean(sampleMMD2NoNaN));
        computedStats.setStdMMD(DataUtils.getStdDev(sampleMMD2NoNaN, computedStats.getMeanMMD()));
        return computedStats;
    }

    /**
     * Calculate the drift relative to the precompute() data.
     * 
     * @param data Test data
     * @param threshold if the probability of "data MMD score >
     *        (mean_mmd+std_mmd*gamma)" is larger than "threshold", we
     *        flag a drift.
     * @param gamma set the threshold to flag drift. If data MMD score >
     *        (mean_mmd+std_mmd*gamma), we assume a drift happened
     * 
     * @return HypothesisTestResult the hypothesis test result
     */

    public HypothesisTestResult calculate(Dataframe data, double threshold, double gamma) {
        final Dataframe numericData = data.getNumericColumns();
        final List<String> colNames = numericData.getRawColumnNames();
        final int numColumns = colNames.size();

        Dataframe xIn = fitStats.isDeltaStat() ? delta(numericData, numColumns) : numericData;

        // Important! Must use the same random seed to regenerate the waveNum and bias
        // values
        final Random rg = new Random(fitStats.getRandomSeed());

        final double[][] waveNum = getWaveNum(numColumns, rg, fitStats.getnMode());
        final double[][] bias = getBias(rg, fitStats.getnMode());

        final int numRows = xIn.getRowDimension();

        final List<List<Value>> xInRows = xIn.getRows();

        final double[][] x1 = getXScaled(numColumns, numRows, xInRows, fitStats.getScale());

        // # 3. compute random Fourier mode
        double[] aComp = randomFourierCoefficients(x1, waveNum, bias, numRows, fitStats.getnMode());

        // # 4. compute mmd score
        // mmd = ((self.learned_params["A_ref"] - a_comp) ** 2).sum()

        double mmd = 0.0;
        for (int c = 0; c < fitStats.getnMode(); c++) {
            final double diff = fitStats.getaRef()[c] - aComp[c];
            final double term = diff * diff;
            mmd += term;
        }

        // drift_score = (mmd - self.learned_params["mean_mmd"]) / self.learned_params[
        // "std_mmd"
        // ]

        final double driftScore = Math.max((mmd - fitStats.getMeanMMD()) / fitStats.getStdMMD(), 0);
        final double cdf = normalDistribution.cumulativeProbability(gamma - driftScore);
        double pValue = 1.0 - cdf;
        return new HypothesisTestResult(driftScore, pValue, pValue > threshold);
    }

    private static Dataframe delta(Dataframe data, final int numColumns) {
        final Dataframe xIn;
        xIn = data.tail(data.getRowDimension() - 1);
        for (int r = 0; r < xIn.getRowDimension(); r++) {
            List<Value> row1Values = xIn.getRow(r);

            List<Value> row2Values = data.getRow(r);
            for (int c = 0; c < numColumns; c++) {
                final double row1Data = row1Values.get(c).asNumber();
                final double row2Data = row2Values.get(c).asNumber();
                final double newData = row1Data - row2Data;
                xIn.setValue(r, c, new Value(newData));
            }
        }
        return xIn;
    }

    private static double[][] getWaveNum(final int numColumns, final Random rg, final int n_mode) {
        // wave_num = np.random.randn(x_in.shape[1], self.n_mode)

        final double[][] waveNum = new double[numColumns][n_mode];
        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < n_mode; j++) {
                waveNum[i][j] = rg.nextGaussian();
            }
        }
        return waveNum;
    }

    private static double[][] getBias(final Random rg, final int n_mode) {
        // bias = np.random.rand(1, self.n_mode) * 2.0 * np.pi

        final double[][] bias = new double[1][n_mode];
        for (int i = 0; i < n_mode; i++) {
            bias[0][i] = rg.nextDouble() * 2.0 * Math.PI;
        }
        return bias;
    }

    // def _random_fourier_coefficients(self, x, wave_num, bias, n_mode):

    private static double[] randomFourierCoefficients(double[][] x, double[][] waveNum, double[][] bias, int ndata,
            final int n_mode) {
        // r_cos = np.cos(np.matmul(x, wave_num) + bias.repeat(x.shape[0], 0))

        final Array2DRowRealMatrix xMatrix = new Array2DRowRealMatrix(x);
        final Array2DRowRealMatrix waveNumMatrix = new Array2DRowRealMatrix(waveNum);
        final Array2DRowRealMatrix product = xMatrix.multiply(waveNumMatrix);

        final double[][] rCos = new double[ndata][n_mode];
        for (int r = 0; r < ndata; r++) {
            for (int c = 0; c < n_mode; c++) {
                final double entry = product.getEntry(r, c);
                final double newEntry = entry + bias[0][c];
                rCos[r][c] = Math.cos(newEntry);
            }
        }

        // a_ref = r_cos.mean(0) * np.sqrt(2 / self.n_mode)
        final double[] aRef = new double[n_mode];
        final double multiplier = Math.sqrt(2.0 / n_mode);
        for (int c = 0; c < n_mode; c++) {
            double sum = 0.0;
            for (int r = 0; r < ndata; r++) {
                sum += rCos[r][c];
            }

            aRef[c] = (sum / ndata) * multiplier;
        }

        return aRef;
    }

    private static double[][] getXScaled(final int numColumns, final int ndata, final List<List<Value>> x1,
            final double[] scaleArray) {
        final double[][] x1Scaled = new double[ndata][numColumns];
        for (int row = 0; row < ndata; row++) {
            final List<Value> rowValues = x1.get(row);
            for (int col = 0; col < numColumns; col++) {
                final Value val = rowValues.get(col);
                final double colDouble = val.asNumber();
                final double scaledColDouble = colDouble / scaleArray[col];
                x1Scaled[row][col] = scaledColDouble;
            }
        }
        return x1Scaled;
    }

}
