package org.kie.trustyai.metrics.drift.fouriermmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;

public class FourierMMD {

    private boolean deltaStat;
    private int n_test;
    private int n_window;
    private double sig;
    private int randomSeed;
    private int n_mode;
    private double threshold;
    private double gamma;

    private Mean mean = new Mean();

    private boolean isBiasCorrected = false;
    private StandardDeviation std = new StandardDeviation(isBiasCorrected);

    private NormalDistribution normalDistribution = new NormalDistribution();

    private FourierMMDFitting fitStats = new FourierMMDFitting();

    public FourierMMD(
            double threshold,
            int n_window,
            int n_test,
            int n_mode,
            int randomSeed,
            double sig,
            boolean deltaStat,
            double gamma) {
        this.deltaStat = deltaStat;
        this.n_test = n_test;
        this.n_window = n_window;
        this.sig = sig;
        this.n_mode = n_mode;
        this.threshold = threshold;
        this.randomSeed = randomSeed;
        this.gamma = gamma;
    }

    public FourierMMD(Dataframe dfTrain) {

        this(0.8, 168, 100, 512, 1234, 10.0, true, 1.5);

        precompute(dfTrain);
    }

    public FourierMMD(FourierMMDFitting fourierMMDFitting) {

        this(0.8, 168, 100, 512, 1234, 10.0, true, 1.5);

        fitStats = fourierMMDFitting;
    }

    // def learn(self, data: pd.DataFrame):
    public FourierMMDFitting precompute(Dataframe data) {

        // save randomSeed in fitStats for execute() use
        fitStats.randomSeed = this.randomSeed;

        final Dataframe numericData = getNumericColumns(data);

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

        // n_test = self.n_test
        // window_size = self.n_window

        // # learn normalization scale
        // self.learned_params["scale"] = np.expand_dims(x_in.std(0) * self.sig, 0)

        final Dataframe sd = xIn.std();

        final Function<Value, Value> multiply = new Multiply(sig);
        final int row2 = 0;
        sd.transformRow(row2, multiply);

        fitStats.scale = sd;

        // # Random Fourier mode for reference data
        // # 1. generate random wavenumber and biases
        // np.random.seed(self.seed)

        final RandomGenerator rg = new JDKRandomGenerator();

        // Init the RNG to the same seed that will be used for the execute() method
        // waveNum (theta) and bias (b) must be the same for precompute() and execute()
        rg.setSeed(this.randomSeed);

        final double[][] waveNum = getWaveNum(numColumns, rg);

        final double[][] bias = getBias(rg);

        // # 2. sample the data set
        // ndata = self.n_window * self.n_test

        final int ndata = n_window * n_test;

        // ndata = np.min([ndata, x_in.shape[0]])

        final int ndata2 = Math.min(ndata, numRows);

        // idx = np.random.choice(x_in.shape[0], ndata, replace=False)

        assert ndata2 <= numRows;

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

        final double[] scaleArray = getScaleArray(numColumns);

        final double[][] x1Scaled = getX1Scaled(numColumns, ndata2, x1, scaleArray);

        // # 3. compute random Fourier mode

        double[] aRef = randomFourierCoefficients(x1Scaled, waveNum, bias, ndata2);

        // self.learned_params["A_ref"] = a_ref

        fitStats.aRef = aRef;

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

            final double[] aComp = randomFourierCoefficients(xWindowed, waveNum, bias, n_window);

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

        fitStats.mean_mmd = 0.0;
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

        fitStats.mean_mmd = mean.evaluate(sampleMMD2NoNaN);

        // self.learned_params["std_mmd"] = np.nanstd(np.array(sample_mmd))

        fitStats.std_mmd = std.evaluate(sampleMMD2NoNaN);

        // return self.learned_params

        return fitStats;
    }

    public FourierMMDResult calculate(Dataframe data) {

        // def execute(self, data: pd.DataFrame):
        // mmd = []
        // computed_values = {}

        final Dataframe numericData = getNumericColumns(data);

        final List<String> colNames = numericData.getColumnNames();
        final int numColumns = colNames.size();

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

        // # 1. re-generate random wavenumber and biases
        // np.random.seed(self.seed)

        final RandomGenerator rg = new JDKRandomGenerator();

        // Important! Must use the same random seed to regenerate the waveNum and bias
        // values
        rg.setSeed(fitStats.randomSeed);

        final double[][] waveNum = getWaveNum(numColumns, rg);

        final double[][] bias = getBias(rg);

        final double[] scaleArray = getScaleArray(numColumns);

        final int numRows = xIn.getRowDimension();

        final List<List<Value>> xInRows = xIn.getRows();

        final double[][] x1 = getX1Scaled(numColumns, numRows, xInRows,
                scaleArray);

        // # 3. compute random Fourier mode

        double[] aComp = randomFourierCoefficients(x1, waveNum, bias, numRows);

        // # 4. compute mmd score
        // mmd = ((self.learned_params["A_ref"] - a_comp) ** 2).sum()

        double mmd = 0.0;
        for (int c = 0; c < n_mode; c++) {
            final double diff = fitStats.aRef[c] - aComp[c];
            final double term = diff * diff;
            mmd += term;
        }

        // drift_score = (mmd - self.learned_params["mean_mmd"]) / self.learned_params[
        // "std_mmd"
        // ]

        final double driftScore = (mmd - fitStats.mean_mmd) / fitStats.std_mmd;

        // drift_score = np.max([0, drift_score])

        final double driftScoreGE0;
        if (driftScore < 0.0) {
            driftScoreGE0 = 0.0;
        } else {
            driftScoreGE0 = driftScore;
        }

        final FourierMMDResult retval = new FourierMMDResult();

        // computed_values["score"] = drift_score

        retval.computedValuesScore = driftScoreGE0;

        // magnitude = 1-st.norm.cdf(-score + self.gamma)

        final double cdf = normalDistribution.cumulativeProbability(this.gamma - driftScoreGE0);

        retval.magnitude = (1.0 - cdf);

        // flag = True if magnitude > self.threshold else False

        if (retval.magnitude > this.threshold) {
            retval.drift = true;
        } else {
            retval.drift = false;
        }

        // return {
        // "drift": flag,
        // "magnitude": magnitude,
        // "computed_values": computed_values,
        // }

        return retval;
    }

    private static Dataframe getNumericColumns(Dataframe input) {
        final List<Type> colTypes = input.getColumnTypes();

        final List<Integer> dropColumns = new ArrayList<Integer>(colTypes.size());

        for (int col = 0; col < colTypes.size(); col++) {

            final Type type = colTypes.get(col);
            if (type != Type.NUMBER) {
                dropColumns.add(col);
            }
        }

        final Dataframe retval = input.copy();
        retval.dropColumns();

        assert dropColumns.size() < colTypes.size();

        return retval;
    }

    private Dataframe delta(Dataframe data, final int numColumns) {
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

    private double[][] getWaveNum(final int numColumns, final RandomGenerator rg) {
        // wave_num = np.random.randn(x_in.shape[1], self.n_mode)

        final double[][] waveNum = new double[numColumns][n_mode];
        for (int i = 0; i < numColumns; i++) {
            for (int j = 0; j < n_mode; j++) {
                waveNum[i][j] = rg.nextGaussian();
            }
        }
        return waveNum;
    }

    private double[][] getBias(final RandomGenerator rg) {
        // bias = np.random.rand(1, self.n_mode) * 2.0 * np.pi

        final double[][] bias = new double[1][n_mode];
        for (int i = 0; i < n_mode; i++) {
            bias[0][i] = rg.nextDouble() * 2.0 * Math.PI;
        }
        return bias;
    }

    // def _random_fourier_coefficients(self, x, wave_num, bias, n_mode):

    private double[] randomFourierCoefficients(double[][] x, double[][] waveNum, double[][] bias, int ndata) {
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

    private double[] getScaleArray(final int numColumns) {
        // x1 = x1 / self.learned_params["scale"].repeat(x1.shape[0], 0)

        final List<Value> scale = fitStats.scale.getRow(0);

        final double[] scaleArray = new double[numColumns];

        for (int i = 0; i < numColumns; i++) {
            scaleArray[i] = scale.get(i).asNumber();
        }
        return scaleArray;
    }

    private double[][] getX1Scaled(final int numColumns, final int ndata2, final List<List<Value>> x1,
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
