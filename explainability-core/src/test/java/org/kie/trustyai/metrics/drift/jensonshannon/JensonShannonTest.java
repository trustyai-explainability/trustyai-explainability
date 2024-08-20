package org.kie.trustyai.metrics.drift.jensonshannon;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.explainability.model.tensor.TensorFactory;
import org.kie.trustyai.explainability.utils.BufferedImageUtils;
import org.kie.trustyai.metrics.utils.ArrayGenerators;

import static org.junit.jupiter.api.Assertions.*;

public class JensonShannonTest {

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
        List<BufferedImage> imagesHyp = generateImage(100, 100);

        Tensor<Double> ref = BufferedImageUtils.preprocessImages(imagesRef);
        Tensor<Double> hyp = BufferedImageUtils.preprocessImages(imagesRef);
        JensonShannonDriftResult result = JensonShannon.calculate(ref, hyp, 0.5);
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

        Tensor<Double> ref = BufferedImageUtils.preprocessImages(imagesRef);
        Tensor<Double> hyp = BufferedImageUtils.preprocessImages(imagesHyp);
        JensonShannonDriftResult result = JensonShannon.calculate(ref, hyp, 0.5);
        assertEquals(0.0, result.getjsStat());
        assertFalse(result.isReject());
    }

    @Test
    void testDiffInputDtypes() {
        List<BufferedImage> imagesRef = generateImage(256, 256);
        Double[][][] tensor3d = new Double[3][256][256];
        List<Double[][][]> imagesHyp = new ArrayList<Double[][][]>();
        imagesHyp.add(tensor3d);

        Tensor<Double> ref = BufferedImageUtils.preprocessImages(imagesRef);
        Tensor<Double> hyp = BufferedImageUtils.preprocessImages(imagesRef);
        JensonShannonDriftResult result = JensonShannon.calculate(ref, hyp, 0.5);
        assertNotNull(result);
    }

    @Test
    void test4DTensors() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 8 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 8 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp);

        JensonShannonDriftResult result = JensonShannon.calculate(ref, hyp, 0.5);
        assertEquals(0.0, result.getjsStat());
        assertFalse(result.isReject());
    }

    @Test
    void testShapeMismatch() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 8 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 9 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp);

        assertThrows(IllegalArgumentException.class, () -> JensonShannon.calculate(ref, hyp, 0.5));
    }

    @Test
    void testDimensionMismatch() {
        Double[][][] tensorRef = ArrayGenerators.get3DDoubleArr(new int[] { 5, 6, 7 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 5, 6, 7, 9 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp);

        assertThrows(IllegalArgumentException.class, () -> JensonShannon.calculate(ref, hyp, 0.5));
    }

    @Test
    void testRuntime() {
        Double[][][][] tensorRef = ArrayGenerators.get4DDoubleArr(new int[] { 64, 3, 256, 256 });
        Double[][][][] tensorHyp = ArrayGenerators.get4DDoubleArr(new int[] { 64, 3, 256, 256 });

        Tensor<Double> ref = TensorFactory.fromArray(tensorRef);
        Tensor<Double> hyp = TensorFactory.fromArray(tensorHyp);

        JensonShannon.calculate(ref, hyp, 0.5);
    }
}
