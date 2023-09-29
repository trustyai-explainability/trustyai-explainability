package org.kie.trustyai.metrics.drift.fouriermmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.utils.DataUtils;

public class FourierMMD {

    private NormalDistribution normalDistribution = new NormalDistribution();
    private Map<String, Object> fitStats;

    // private FourierMMDFitting fitStats = new FourierMMDFitting();

    // public FourierMMD() {
    // }

    public FourierMMD(Dataframe dfTrain, boolean deltaStat, int n_test, int n_window, double sig, int randomSeed,
            int n_mode) {

        FourierMMDFitting fourierMMDFitting = precompute(dfTrain, deltaStat, n_test, n_window, sig, randomSeed, n_mode);
        fitStats = fourierMMDFitting.fitStats;
    }

    public FourierMMD(FourierMMDFitting fourierMMDFitting) {

        fitStats = fourierMMDFitting.fitStats;
    }

    // def learn(self, data: pd.DataFrame):
    public static FourierMMDFitting precompute(Dataframe data, boolean deltaStat, int n_test, int n_window, double sig,
            int randomSeed, int n_mode) {

        Map<String, Object> computedStats = new HashMap<>();
        // save randomSeed in fitStats for execute() use
        computedStats.put("randomSeed", randomSeed);
        computedStats.put("deltaStat", deltaStat);
        computedStats.put("n_mode", n_mode);

        final Dataframe numericData = data.getNumericColumns();

        final List<String> columns = numericData.getColumnNames();

        final int numColumns = columns.size();

        // if self.delta_stat:
        // x_in = data[1:, :] - data[:-1, :]
        // else:
        // x_in = data

        final Dataframe xIn;
        if (deltaStat) {
            xIn = delta(numericData, numColumns);
        } else {
            xIn = numericData;
        }

        final int numRows = xIn.getRowDimension();
        if (numRows == 0) {
            throw new IllegalArgumentException("Dataframe is empty");
        }

        // n_test = self.n_test
        // window_size = self.n_window

        // # learn normalization scale
        // self.learned_params["scale"] = np.expand_dims(x_in.std(0) * self.sig, 0)

        final Dataframe sd = xIn.std();

        final Function<Value, Value> multiply = (Value v) -> new Value(v.asNumber() * sig);

        final int row2 = 0;
        sd.transformRow(row2, multiply);
        final double[] scaleArray = dfToArray(sd, numColumns);
        computedStats.put("scale", scaleArray);

        // # Random Fourier mode for reference data
        // # 1. generate random wavenumber and biases
        // np.random.seed(self.seed)

        // Init the RNG to the same seed that will be used for the execute() method
        // waveNum (theta) and bias (b) must be the same for precompute() and execute()
        final Random rg = new Random(randomSeed);

        final double[][] waveNum = getWaveNum(numColumns, rg, n_mode);

        final double[][] bias = getBias(rg, n_mode);

        // # 2. sample the data set
        // ndata = self.n_window * self.n_test

        final int ndata = n_window * n_test;

        // ndata = np.min([ndata, x_in.shape[0]])

        final int ndata2 = Math.min(ndata, numRows);
        if (ndata2 <= n_window) {
            throw new IllegalArgumentException("n_window must be less than " + ndata2);
        }

        // idx = np.random.choice(x_in.shape[0], ndata, replace=False)

        final boolean[] done = new boolean[numRows];
        Arrays.fill(done, false);

        final int rand[] = new int[ndata2];
        for (int posn = 0; posn < ndata2;) {
            final int tentativeRandom = (int) (rg.nextDouble() * numRows);
            if (done[tentativeRandom]) {
                continue;
            }

            done[tentativeRandom] = true;

            rand[posn] = tentativeRandom;
            posn += 1;
        }

        // x1 = x_in[idx, :]

        final List<List<Value>> x1 = new ArrayList<List<Value>>(ndata2);

        for (int i = 0; i < ndata2; i++) {
            x1.add(xIn.getRow(rand[i]));
        }

        final double[][] x1Scaled = getXScaled(numColumns, ndata2, x1, scaleArray);

        // # 3. compute random Fourier mode

        double[] aRef = randomFourierCoefficients(x1Scaled, waveNum, bias, ndata2, n_mode);

        // self.learned_params["A_ref"] = a_ref

        computedStats.put("aRef", aRef);

        // # 4. compute reference mmd score
        // sample_mmd = []

        final double[] sampleMMD = new double[n_test];

        // for i in range(n_test):

        for (int i = 0; i < n_test; i++) {

            // idx = np.random.randint(x_in.shape[0] - window_size)

            final int indexSize = ndata2 - n_window;
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

        computedStats.put("mean_mmd", DataUtils.getMean(sampleMMD2NoNaN));

        // self.learned_params["std_mmd"] = np.nanstd(np.array(sample_mmd))

        computedStats.put("std_mmd", DataUtils.getStdDev(sampleMMD2NoNaN, (double) computedStats.get("mean_mmd")));

        // return self.learned_params

        return new FourierMMDFitting(computedStats);
    }

    public FourierMMDResult calculate(Dataframe data, double threshold, double gamma) {

        // def execute(self, data: pd.DataFrame):
        // mmd = []
        // computed_values = {}

        final Dataframe numericData = data.getNumericColumns();

        final List<String> colNames = numericData.getColumnNames();
        final int numColumns = colNames.size();

        // if self.delta_stat:
        // x_in = data[1:, :] - data[:-1, :]
        // else:
        // x_in = data

        final Dataframe xIn;
        if ((boolean) fitStats.get("deltaStat")) {
            xIn = delta(numericData, numColumns);
        } else {
            xIn = numericData;
        }

        // # 1. re-generate random wavenumber and biases
        // np.random.seed(self.seed)

        // Important! Must use the same random seed to regenerate the waveNum and bias
        // values
        final Random rg = new Random((int) fitStats.get("randomSeed"));

        final double[][] waveNum = getWaveNum(numColumns, rg, (int) fitStats.get("n_mode"));

        final double[][] bias = getBias(rg, (int) fitStats.get("n_mode"));

        final int numRows = xIn.getRowDimension();

        final List<List<Value>> xInRows = xIn.getRows();

        final double[][] x1 = getXScaled(numColumns, numRows, xInRows,
                (double[]) fitStats.get("scale"));

        // # 3. compute random Fourier mode

        double[] aComp = randomFourierCoefficients(x1, waveNum, bias, numRows, (int) fitStats.get("n_mode"));

        // # 4. compute mmd score
        // mmd = ((self.learned_params["A_ref"] - a_comp) ** 2).sum()

        double mmd = 0.0;
        for (int c = 0; c < (int) fitStats.get("n_mode"); c++) {
            final double diff = ((double[]) fitStats.get("aRef"))[c] - aComp[c];
            final double term = diff * diff;
            mmd += term;
        }

        // drift_score = (mmd - self.learned_params["mean_mmd"]) / self.learned_params[
        // "std_mmd"
        // ]

        final double driftScore = (mmd - (double) fitStats.get("mean_mmd")) / (double) fitStats.get("std_mmd");

        // drift_score = np.max([0, drift_score])

        final double driftScoreGE0;
        if (driftScore < 0.0) {
            driftScoreGE0 = 0.0;
        } else {
            driftScoreGE0 = driftScore;
        }

        final FourierMMDResult retval = new FourierMMDResult();

        // computed_values["score"] = drift_score

        retval.relativeMMDScore = driftScoreGE0;

        // magnitude = 1-st.norm.cdf(-score + self.gamma)

        final double cdf = normalDistribution.cumulativeProbability(gamma - driftScoreGE0);

        retval.pValue = (1.0 - cdf);

        // flag = True if magnitude > self.threshold else False

        if (retval.pValue > threshold) {
            retval.drifted = true;
        } else {
            retval.drifted = false;
        }

        // return {
        // "drift": flag,
        // "magnitude": magnitude,
        // "computed_values": computed_values,
        // }

        return retval;
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

    private static double[] randomFourierCoefficients(double[][] x, double[][] waveNum, double[][] bias, int ndata, final int n_mode) {
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

    private static double[] dfToArray(Dataframe df, final int numColumns) {
        // x1 = x1 / self.learned_params["scale"].repeat(x1.shape[0], 0)

        final List<Value> scale = df.getRow(0);

        final double[] scaleArray = new double[numColumns];

        for (int i = 0; i < numColumns; i++) {
            scaleArray[i] = scale.get(i).asNumber();
        }
        return scaleArray;
    }

    private static double[][] getXScaled(final int numColumns, final int ndata2, final List<List<Value>> x1,
            final double[] scaleArray) {
        final double[][] x1Scaled = new double[ndata2][numColumns];
        for (int row = 0; row < ndata2; row++) {
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
