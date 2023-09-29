package org.kie.trustyai.metrics.drift.kstest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.JDKRandomGenerator;
import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.Feature;
import org.kie.trustyai.explainability.model.FeatureFactory;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.metrics.drift.HypothesisTestResult;

import static org.junit.jupiter.api.Assertions.*;

class KSTestTest {

    /*
     * 1. Univariate Normal mean shift
     * 2. Univariate Normal no shift
     * 3. Univariate Normal variance shift
     */
    public static final int colSize = 4;
    public static final int sampleSize = 10000;
    int randomSeed = 0;
    double[][] dists = getNormalDistributions(randomSeed);

    // Produces 4 Normal distribution with various means and stds
    public double[][] getNormalDistributions(int randomSeed) {
        JDKRandomGenerator randomGen = new JDKRandomGenerator(randomSeed);
        dists = new double[colSize][sampleSize];
        NormalDistribution nd1 = new NormalDistribution(randomGen, 0.0, 1.0);
        NormalDistribution nd2 = new NormalDistribution(randomGen, 1.0, 1.0);
        NormalDistribution nd3 = new NormalDistribution(randomGen, 1.0, 1.0);
        NormalDistribution nd4 = new NormalDistribution(randomGen, 0.0, 2.0);

        dists[0] = nd1.sample(sampleSize);
        dists[1] = nd2.sample(sampleSize);
        dists[2] = nd3.sample(sampleSize);
        dists[3] = nd4.sample(sampleSize);

        return dists;
    }

    // generate single column DF from mock data
    public Dataframe generate(int col) {
        List<PredictionInput> ps = new ArrayList<>();
        for (int i = 0; i < sampleSize; i++) {
            ps.add(new PredictionInput(List.of(FeatureFactory.newNumericalFeature(String.valueOf(i), dists[col][i]))));
        }
        return Dataframe.createFromInputs(ps);
    }

    public Dataframe generate(Integer[] colIdxs, String colPrefix) {
        List<PredictionInput> ps = new ArrayList<>();
        for (int i = 0; i < sampleSize; i++) {
            List<Feature> fs = new ArrayList<>();
            AtomicInteger colName = new AtomicInteger(0);
            for (int colIdx : colIdxs) {
                fs.add(FeatureFactory.newNumericalFeature(colPrefix + colName.getAndIncrement(), dists[colIdx][i]));
            }
            ps.add(new PredictionInput(fs));
        }
        return Dataframe.createFromInputs(ps);
    }

    @Test
    void testUnivariateNormalDistributionsNoShift() {
        Dataframe data1 = generate(1); // N(1,1)
        Dataframe data2 = generate(2); // N(1,1)
        KSTest ks = new KSTest();
        List<String> names = data1.getColumnNames();
        HashMap<String, HypothesisTestResult> result = ks.calculate(data1, data2, 0.05);
        assertEquals(1, result.size());
        assertTrue(result.get(names.get(0)).getpValue() >= 0.01);
        assertTrue(result.get(names.get(0)).getStatVal() > 0.0);
        assertEquals(false, result.get(names.get(0)).isReject());
    }

    @Test
    void testUnivariateNormalDistributionsMeanShift() {
        Dataframe data1 = generate(0); // N(0,1)
        Dataframe data2 = generate(1); // N(1,1)
        KSTest ks = new KSTest();
        List<String> names = data1.getColumnNames();
        HashMap<String, HypothesisTestResult> result = ks.calculate(data1, data2, 0.05);
        assertEquals(1, result.size());
        assertTrue(result.get(names.get(0)).getpValue() <= 0.01);
        assertTrue(result.get(names.get(0)).getStatVal() > 0.0);
        assertEquals(true, result.get(names.get(0)).isReject());
    }

    @Test
    void testUnivariateNormalDistributionsVarianceShift() {
        Dataframe data1 = generate(0); // N(0,1)
        Dataframe data2 = generate(3); // N(0,2)
        KSTest ks = new KSTest();
        List<String> names = data1.getColumnNames();
        HashMap<String, HypothesisTestResult> result = ks.calculate(data1, data2, 0.05);
        assertEquals(1, result.size());
        assertTrue(result.get(names.get(0)).getpValue() <= 0.01);
        assertTrue(result.get(names.get(0)).getStatVal() > 0.0);
        assertEquals(true, result.get(names.get(0)).isReject());
    }

    @Test
    void testMultiColumnKSTest() {
        dists = getNormalDistributions(34);
        Integer[] idx1 = new Integer[3]; //0, 1,2
        Integer[] idx2 = new Integer[3]; //1,2,3
        for (int i = 0; i < colSize - 1; i++) {
            idx1[i] = i;
            idx2[i] = i + 1;
        }

        Dataframe data1 = generate(idx1, "normal_dist");
        Dataframe data2 = generate(idx2, "normal_dist");
        KSTest ks = new KSTest();
        List<String> names = data1.getColumnNames();
        HashMap<String, HypothesisTestResult> result = ks.calculate(data1, data2, 0.05);

        assertTrue(result.get(names.get(0)).getpValue() <= 0.05);
        assertTrue(result.get(names.get(0)).getStatVal() > 0.0);
        assertTrue(result.get(names.get(1)).getpValue() >= 0.05);
        assertTrue(result.get(names.get(1)).getStatVal() > 0.0);
        assertTrue(result.get(names.get(2)).getpValue() <= 0.05);
        assertTrue(result.get(names.get(2)).getStatVal() > 0.0);
    }

}
