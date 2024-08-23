package org.kie.trustyai.metrics.drift.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.explainability.model.tensor.TensorFactory;

import static org.junit.jupiter.api.Assertions.*;

public class ImageDriftTest {

    Double[][][] get3DArr(int[] dimension) {
        int a = dimension[0];
        int b = dimension[1];
        int c = dimension[2];
        Double[][][] arr = new Double[a][b][c];
        Double idx = 0.0;
        for (int i = 0; i < a; i++) {
            for (int ii = 0; ii < b; ii++) {
                for (int iii = 0; iii < c; iii++) {
                    arr[i][ii][iii] = idx;
                    idx += 1;
                }
            }
        }
        return arr;
    }

    Tensor<Double> create3DTensor(){
        int[] dimension = {256, 256, 3};
        Double[][][] arr = get3DArr(dimension);
        Tensor<Double> tensor3d = TensorFactory.fromArray(arr);
        return tensor3d;
    }

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
    void testGetBufferedImageDataFrame() {
        List<BufferedImage> bufferedImages = generateImage(100, 100);
        List<Tensor<Double>> tensor3dRef = ImageDrift.preprocessImages(bufferedImages);
        Dataframe df = ImageDrift.getDataframe(tensor3dRef);
        assertNotNull(df);
        assertEquals(df.getColumnDimension(), 3);
        assertEquals(bufferedImages.get(0).getWidth() * bufferedImages.get(0).getHeight(), df.getRowDimension());
    }

    @Test
    void testGetTensor3dImageDataFrame() {
        Tensor<Double> tensor3d = create3DTensor();
        List<Tensor<Double>> tensor3dImages = new ArrayList<Tensor<Double>>();
        tensor3dImages.add(tensor3d);
        List<Tensor<Double>> tensor3dRef = ImageDrift.preprocessImages(tensor3dImages);
        Dataframe df = ImageDrift.getDataframe(tensor3dRef);
        assertNotNull(df);
        assertEquals(df.getColumnDimension(), 3);
        assertEquals(tensor3d.getDimensions(0) * tensor3d.getDimensions(1), df.getRowDimension());
    }

    @Test
    void testSameBufferedImages() {
        List<BufferedImage> imagesRef = generateImage(100, 100);
        List<BufferedImage> imagesHyp = generateImage(100, 100);
        ImageDriftResult result = ImageDrift.calculate(imagesRef, imagesHyp, 0.5);
        assertEquals(0.0, result.getjsStat());
        assertFalse(result.isReject());
    }

    @Test
    void testSameTensor3dImages() {
        Tensor<Double> tensor3d = create3DTensor();
        List<Tensor<Double>> imagesRef = new ArrayList<Tensor<Double>>();
        imagesRef.add(tensor3d);
        List<Tensor<Double>> imagesHyp = new ArrayList<Tensor<Double>>();
        imagesHyp.add(tensor3d);
        ImageDriftResult result = ImageDrift.calculate(imagesRef, imagesHyp, 0.5);
        assertEquals(0.0, result.getjsStat());
        assertFalse(result.isReject());
    }

    @Test
    void testDiffInputDtypes() {
        List<BufferedImage> imagesRef = generateImage(256, 256);
        Tensor<Double> tensor3d = create3DTensor();
        List<Tensor<Double>> imagesHyp = new ArrayList<Tensor<Double>>();
        imagesHyp.add(tensor3d);
        ImageDriftResult result = ImageDrift.calculate(imagesRef, imagesHyp, 0.5);
        assertNotNull(result);
    }
}
