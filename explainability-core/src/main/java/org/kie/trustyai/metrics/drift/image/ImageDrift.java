package org.kie.trustyai.metrics.drift.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.explainability.model.Type;
import org.kie.trustyai.explainability.model.Value;
import org.kie.trustyai.explainability.model.dataframe.Dataframe;
import org.kie.trustyai.metrics.drift.utils.KLDivergence;
import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.explainability.model.tensor.TensorFactory;

/**
 * Jensen-Shannon divergence to calculate image data drift.
 * See <a href="https://en.wikipedia.org/wiki/Jensen%E2%80%93Shannon_divergence">Jensen–Shannon divergence</a>
 */
public class ImageDrift {
    /**
     * Converts an image of type BufferedImage to double[][][]
     *
     * @param image An image of type BufferedImage
     */
    private static Tensor<Double> convertToTensor3d(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Double[][][] arr = new Double[width][height][3];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = image.getRGB(i, j);
                arr[i][j][0] = ((pixel >> 16) & 0xff) / 255.0;
                arr[i][j][1] = ((pixel >> 8) & 0xff) / 255.0;
                arr[i][j][2] = (pixel & 0xff) / 255.0;
            }
        }
        Tensor<Double> tensor3d = TensorFactory.fromArray(arr);
        return tensor3d;
    }

    /**
     * Takes as input a list of images and converts them into a list of 3d arrays.
     *
     * @param images A list of images of data type BufferedImage or Tensor<Double>?
     */
    @SuppressWarnings("unchecked")
    public static List<Tensor<Double>> preprocessImages(List<?> images) {
        List<Tensor<Double>> preprocessedImgs = new ArrayList<>();
        for (Object image : images) {
            if (image instanceof BufferedImage) {
                preprocessedImgs.add(convertToTensor3d((BufferedImage) image));
            } else if (image instanceof Tensor) {
                preprocessedImgs.add((Tensor<Double>) image);
            } else {
                throw new IllegalArgumentException("Unsupported image format.");
            }
        }
        return preprocessedImgs;

    }

    /**
     * Takes as input of list of images and converts them into a dataframe.
     *
     * @param images A list of images of data type Tensor<Double>
     */
    public static Dataframe getDataframe(List<Tensor<Double>> images) {
        Dataframe dataframe = new Dataframe();
        List<Value> redCol = new ArrayList<>();
        List<Value> greenCol = new ArrayList<>();
        List<Value> blueCol = new ArrayList<>();

        for (Tensor<Double> image : images) {
            int width = image.getDimensions(0);
            int height = image.getDimensions(1);
            int channels = image.getDimensions(2);
            if (channels != 3) {
                throw new IllegalArgumentException("Each pixel must have exactly 3 color channels");
            }
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {

                    redCol.add(new Value(image.getElement(i, j, 0)));
                    greenCol.add(new Value(image.getElement(i, j, 1)));
                    blueCol.add(new Value(image.getElement(i, j, 2)));

                }
            }
            dataframe.addColumn("Red", Type.NUMBER, redCol);
            dataframe.addColumn("Green", Type.NUMBER, greenCol);
            dataframe.addColumn("Blue", Type.NUMBER, blueCol);

        }
        return dataframe;
    }

    /**
     * Calculates the Jenson-Shannon divergence between two pixel arrays.
     *
     * @param p1 The reference pixel array.
     * @param p2 The hypothesis pixel array.
     * @return The Jenson-Shannon divergence.
     */
    public static double jensonShannonDivergence(double[] p1, double[] p2) {
        double[] m = new double[p1.length];
        for (int i = 0; i < p1.length; i++) {
            m[i] = (p1[i] + p2[i]) / 2.0;
        }
        KLDivergence kl_div1 = new KLDivergence(p1, m);
        KLDivergence kl_div2 = new KLDivergence(p2, m);
        return (kl_div1.calculate() + kl_div2.calculate()) / 2.0;
    }

    /**
     * Calculates the average Jensen-Shannon divergence over RGB values between a reference and hypothesis image.
     * If it is above the threshold, the hypothesis image is said to be different than the reference image.
     *
     * @param <T>
     * @param <T>
     *
     * @param reference The reference image.
     * @param hypothesis The hypothesis image.
     * @param threshold A threshold to determine whether the hypothesis image is different from the reference image.
     * @return The image drift result.
     */

    public static ImageDriftResult calculate(List<?> references, List<?> hypotheses, double threshold) {
        List<Tensor<Double>> tensor3dRef = preprocessImages(references);
        List<Tensor<Double>> tensor3dHyp = preprocessImages(hypotheses);

        Dataframe dfRef = getDataframe(tensor3dHyp);
        Dataframe dfHyp = getDataframe(tensor3dRef);

        double jsStat = 0.0;
        for (int channel = 0; channel < 3; channel++) {
            jsStat += jensonShannonDivergence(
                    dfRef.getColumn(channel).stream().mapToDouble(Value::asNumber).toArray(),
                    dfHyp.getColumn(channel).stream().mapToDouble(Value::asNumber).toArray());
        }
        jsStat /= 3;
        boolean reject = jsStat > threshold;
        return new ImageDriftResult(jsStat, threshold, reject);
    }
}
