package org.kie.trustyai.explainability.utils;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.kie.trustyai.explainability.model.tensor.Tensor;
import org.kie.trustyai.explainability.model.tensor.TensorFactory;

public class ImageUtils {
    /**
     * Converts an image of type BufferedImage to double[][][]
     *
     * @param image An image of type BufferedImage
     */
    public static Tensor<Double> convertToTensor3d(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        Double[][][] tensor3d = new Double[3][width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int pixel = image.getRGB(i, j);
                tensor3d[0][i][j] = ((pixel >> 16) & 0xff) / 255.0;
                tensor3d[1][i][j] = ((pixel >> 8) & 0xff) / 255.0;
                tensor3d[2][i][j] = (pixel & 0xff) / 255.0;
            }
        }
        return TensorFactory.fromArray(tensor3d);
    }

    /**
     * Takes as input a list of images and converts them into a list of 3d arrays.
     *
     * @param images A list of images of data type BufferedImage or double[][][]
     */
    public static Tensor<Double> preprocessImages(List<?> images) {
        List<Tensor<?>> preprocessedImgs = new ArrayList<>();
        for (Object image : images) {
            if (image instanceof BufferedImage) {
                BufferedImage bufferedImage = (BufferedImage) image;
                preprocessedImgs.add(ImageUtils.convertToTensor3d(bufferedImage));
            } else if (image instanceof Tensor<?>) {
                Tensor<?> tensor = (Tensor<?>) image;
                preprocessedImgs.add(tensor);
            } else if (image instanceof Double[][][]) {
                Double[][][] doubles = (Double[][][]) image;
                preprocessedImgs.add(TensorFactory.fromArray(doubles));
            } else {
                throw new IllegalArgumentException("Unsupported image format.");
            }
        }
        return Tensor.stack(preprocessedImgs.toArray(new Tensor[0]));
    }
}
