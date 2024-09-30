package org.kie.trustyai.explainability.model.tensor;

import org.apache.commons.lang3.NotImplementedException;
import org.kie.trustyai.explainability.model.PerturbationContext;
import org.kie.trustyai.explainability.utils.IOUtils;

public class TensorUtilities {

    // fill the input tensor with random values of the same datatype
    public static <T> Tensor<T> randomFill(Tensor<T> tensor, PerturbationContext perturbationContext) {
        Class tensorType = tensor.getDatatype();
        if (tensorType == Integer.class) {
            return (Tensor<T>) randomIntFill(tensor, perturbationContext);
        } else if (tensorType == Long.class) {
            return (Tensor<T>) randomLongFill(tensor, perturbationContext);
        } else if (tensorType == Float.class) {
            return (Tensor<T>) randomFloatFill(tensor, perturbationContext);
        } else if (tensorType == Double.class) {
            return (Tensor<T>) randomDoubleFill(tensor, perturbationContext);
        } else if (tensorType == Boolean.class) {
            return (Tensor<T>) randomBooleanFill(tensor, perturbationContext);
        } else if (tensorType == String.class) {
            return (Tensor<T>) randomStringFill((Tensor<String>) tensor, perturbationContext);
        } else if (tensorType == Byte.class) {
            return (Tensor<T>) randomByteFill(tensor, perturbationContext);
        } else {
            throw new NotImplementedException("Cannot perturb a tensor of type=" + tensorType);
        }
    }

    // individual functions ============================================================================================
    public static Tensor<Integer> randomIntFill(Tensor<?> tensor, PerturbationContext perturbationContext) {
        Integer[] dataArr = new Integer[tensor.getnEntries()];
        for (int i = 0; i < tensor.getnEntries(); i++) {
            dataArr[i] = Integer.valueOf(perturbationContext.getRandom().nextInt());
        }
        return new Tensor<>(dataArr, tensor.getDimensions());
    }

    public static Tensor<Long> randomLongFill(Tensor<?> tensor, PerturbationContext perturbationContext) {
        Long[] dataArr = new Long[tensor.getnEntries()];
        for (int i = 0; i < tensor.getnEntries(); i++) {
            dataArr[i] = Long.valueOf(perturbationContext.getRandom().nextLong());
        }
        return new Tensor<>(dataArr, tensor.getDimensions());
    }

    public static Tensor<Float> randomFloatFill(Tensor<?> tensor, PerturbationContext perturbationContext) {
        Float[] dataArr = new Float[tensor.getnEntries()];
        for (int i = 0; i < tensor.getnEntries(); i++) {
            dataArr[i] = Float.valueOf(perturbationContext.getRandom().nextFloat());
        }
        return new Tensor<>(dataArr, tensor.getDimensions());
    }

    public static Tensor<Double> randomDoubleFill(Tensor<?> tensor, PerturbationContext perturbationContext) {
        Double[] dataArr = new Double[tensor.getnEntries()];
        for (int i = 0; i < tensor.getnEntries(); i++) {
            dataArr[i] = Double.valueOf(perturbationContext.getRandom().nextDouble());
        }
        return new Tensor<>(dataArr, tensor.getDimensions());
    }

    public static Tensor<Boolean> randomBooleanFill(Tensor<?> tensor, PerturbationContext perturbationContext) {
        Boolean[] dataArr = new Boolean[tensor.getnEntries()];
        for (int i = 0; i < tensor.getnEntries(); i++) {
            dataArr[i] = Boolean.valueOf(perturbationContext.getRandom().nextBoolean());
        }
        return new Tensor<>(dataArr, tensor.getDimensions());
    }

    // generates random alphabetical strings, where the ith generated string has the same length as the original ith string
    public static Tensor<String> randomStringFill(Tensor<String> tensor, PerturbationContext perturbationContext) {
        String[] dataArr = new String[tensor.getnEntries()];

        for (int i = 0; i < tensor.getnEntries(); i++) {
            String ithString = (String) tensor.getElement(tensor.getCoordinates(i));
            dataArr[i] = IOUtils.randomString(ithString.length(), perturbationContext.getRandom());
        }
        return new Tensor<>(dataArr, tensor.getDimensions());
    }

    // generates random alphabetical strings, where all strings are of length $strLen
    public static Tensor<String> randomStringFill(Tensor<?> tensor, int strLen, PerturbationContext perturbationContext) {
        String[] dataArr = new String[tensor.getnEntries()];

        for (int i = 0; i < tensor.getnEntries(); i++) {
            dataArr[i] = IOUtils.randomString(strLen, perturbationContext.getRandom());
        }
        return new Tensor<>(dataArr, tensor.getDimensions());
    }

    public static Tensor<Byte> randomByteFill(Tensor<?> tensor, PerturbationContext perturbationContext) {
        Byte[] dataArr = new Byte[tensor.getnEntries()];
        for (int i = 0; i < tensor.getnEntries(); i++) {
            byte[] arr = new byte[1];
            perturbationContext.getRandom().nextBytes(arr);
            dataArr[i] = Byte.valueOf(arr[0]);
        }
        return new Tensor<>(dataArr, tensor.getDimensions());
    }
}
