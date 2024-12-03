package org.kie.trustyai.metrics.drift.jensenshannon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.explainability.model.tensor.TensorFactory;
import org.kie.trustyai.explainability.utils.ImageUtils;
import org.kie.trustyai.metrics.utils.ArrayGenerators;

import static org.junit.jupiter.api.Assertions.*;

public class JensenShannonTest {
    Random rng = new Random(0L);

    // Creates test image
    public List<BufferedImage> generateImage(int width, int height) {
        List<BufferedImage> imageList = new ArrayList<BufferedImage>();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.red);
        g2d.fillRect(0, 0, width / 2, height / 2);
        g2d.setColor(Color.green);
        g2d.fillRect(width / 2, 0, width / 2, height / 2);
        g2d.setColor(Color.blue);
        g2d.fillRect(0, height / 2, width / 2, height / 2);
        g2d.setColor(Color.yellow);
        g2d.fillRect(width / 2, height / 2, width / 2, height / 2);
        g2d.dispose();
        imageList.add(image);
        return imageList;
    }

    @Test
    void testSameBufferedImages() {
        List<BufferedImage> imagesRef = generateImage(100, 100);

        Tensor<Double> ref = ImageUtils.preprocessImages(imagesRef);
        Tensor<Double> hyp = ImageUtils.preprocessImages(imagesRef);
        JensenShannonDriftResult result = JensenShannon.calculate(ref, hyp, 0.5);
        assertEquals(0.0, result.getjsStat());
        assertFalse(result.isReject());
    }

    @Test
    void testSameTensor3dImages() {
        Double[][][] tensor3d = ArrayGenerators.get3DDoubleArr(new int[] { 3, 256, 256 });
        List<Double[][][]> imagesRef = new ArrayList<Double[][][]>();
        imagesRef.add(tensor3d);
        List<Double[][][]> imagesHyp = new ArrayList<Double[][][]>();
        imagesHyp.add(tensor3d);

        Tensor<Double> ref = ImageUtils.preprocessImages(imagesRef);
        int nEntries = ref.getnEntries();
        ref = ref.map(d -> d / nEntries);
        Tensor<Double> hyp = ImageUtils.preprocessImages(imagesHyp).map(d -> d / nEntries);
        ;
        JensenShannonDriftResult result = JensenShannon.calculate(ref, hyp, 0.5);
        assertEquals(0.0, result.getjsStat());
        assertFalse(result.isReject());
    }

    @Test
    void testDiffInputDtypes() {
        List<BufferedImage> imagesRef = generateImage(256, 256);
        Double[][][] tensor3d = new Double[3][256][256];
        List<Double[][][]> imagesHyp = new ArrayList<Double[][][]>();
        imagesHyp.add(tensor3d);

        Tensor<Double> ref = ImageUtils.preprocessImages(imagesRef);
        Tensor<Double> hyp = ImageUtils.preprocessImages(imagesRef);
        JensenShannonDriftResult result = JensenShannon.calculate(ref, hyp, 0.5);
        assertNotNull(result);
    }

    @Test
    void test4DTensors() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 8 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 8 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        int nEntries = ref.getnEntries();
        ref = ref.map(d -> d / nEntries);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp).map(d -> d / nEntries);

        JensenShannonDriftResult result = JensenShannon.calculate(ref, hyp, 0.5);
        assertEquals(0.0, result.getjsStat());
        assertFalse(result.isReject());
    }

    @Test
    void testShapeMismatch() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 8 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 9 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp);

        assertThrows(IllegalArgumentException.class, () -> JensenShannon.calculate(ref, hyp, 0.5));
    }

    @Test
    void testDimensionMismatch() {
        Double[][][] tensorRef = ArrayGenerators.get3DDoubleArr(new int[] { 5, 6, 7 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 9 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp);

        assertThrows(IllegalArgumentException.class, () -> JensenShannon.calculate(ref, hyp, 0.5));
    }

    @Test
    void testBaseline() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 64, 3, 32, 32 });
        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        int nEntries = ref.getnEntries();
        ref = ref.map(d -> d / nEntries);
        rng = new Random(0L);
        JensenShannonBaseline jsb = JensenShannonBaseline.calculate(ref, 100, 32, rng, false);

        assertTrue(jsb.getAvgThreshold() <= jsb.getMaxThreshold());
        assertTrue(jsb.getAvgThreshold() >= jsb.getMinThreshold());
        assertTrue(5000 < jsb.getAvgThreshold() && jsb.getAvgThreshold() < 5100);
    }

    @Test
    void testBaselineNormalized() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 64, 3, 32, 32 });
        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        int nEntries = ref.getnEntries();
        ref = ref.map(d -> d / nEntries);
        rng = new Random(0L);
        JensenShannonBaseline jsb = JensenShannonBaseline.calculate(ref, 100, 32, rng, true);

        assertTrue(jsb.getAvgThreshold() <= jsb.getMaxThreshold());
        assertTrue(jsb.getAvgThreshold() >= jsb.getMinThreshold());
        assertTrue(.05 < jsb.getAvgThreshold() && jsb.getAvgThreshold() < .055);
    }

    @Test
    void testLargeImage() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 64, 3, 128, 128 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 64, 3, 128, 128 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        int nEntries = ref.getnEntries();
        ref = ref.map(d -> d / nEntries);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp).map(d -> d / nEntries);

        rng = new Random(0L);
        JensenShannonBaseline jsb = JensenShannonBaseline.calculate(ref, 10, 32, rng, true);
        JensenShannonDriftResult jsdr = JensenShannon.calculate(ref, hyp, jsb.getMaxThreshold() * 1.5, true);
        assertFalse(jsdr.isReject());
    }

    @Test
    void testLargeImagePerChannel() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 64, 3, 128, 128 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 64, 3, 128, 128 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        int nEntries = ref.getnEntries();
        ref = ref.map(d -> d / nEntries);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp).map(d -> d / nEntries);

        rng = new Random(0L);
        JensenShannonBaseline[] jensenShannonBaselines = JensenShannonBaseline.calculatePerChannel(ref, 10, 32, rng, true);
        double[] perChannelThresholds = Arrays.stream(jensenShannonBaselines).mapToDouble(j -> j.getMaxThreshold() * 1.5).toArray();
        JensenShannonDriftResult[] jsdr = JensenShannon.calculatePerChannel(ref, hyp, perChannelThresholds, true);

        for (int i = 0; i < ref.getDimensions(1); i++) {
            assertFalse(jsdr[i].isReject());
        }
    }
}
