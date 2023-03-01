package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.connectors.kserve.v2.grpc.InferTensorContents;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PayloadParserTest {

    @Test
    void modelInferResponseToPredictionOutputSingle() {

        final Random random = new Random();
        final double value = random.nextDouble();

        InferTensorContents.Builder contents = InferTensorContents.newBuilder()
                .addFp64Contents(value);

        ModelInferResponse.InferOutputTensor outputTensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(1).setContents(contents).build();

        PredictionOutput predictionOutput = PayloadParser.outputTensorToPredictionOutput(outputTensor, null);

        assertEquals(1, predictionOutput.getOutputs().size());
        assertEquals(value, predictionOutput.getOutputs().get(0).getValue().asNumber());
    }

    @Test
    void modelInferResponseToPredictionOutputMulti() {

        final Random random = new Random();
        final List<Double> values = random.doubles(3).boxed().collect(Collectors.toList());
        InferTensorContents.Builder contents = InferTensorContents.newBuilder()
                .addFp64Contents(values.get(0))
                .addFp64Contents(values.get(1))
                .addFp64Contents(values.get(2));

        ModelInferResponse.InferOutputTensor outputTensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(3).setContents(contents).build();

        PredictionOutput predictionOutput = PayloadParser.outputTensorToPredictionOutput(outputTensor, null);

        assertEquals(3, predictionOutput.getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), predictionOutput.getOutputs().get(i).getValue().asNumber());
        }
    }

    @Test
    void modelInferResponseToPredictionOutputFp32() {

        final Random random = new Random();
        final List<Float> values = List.of(random.nextFloat(), random.nextFloat(), random.nextFloat());
        InferTensorContents.Builder contents = InferTensorContents.newBuilder()
                .addFp32Contents(values.get(0))
                .addFp32Contents(values.get(1))
                .addFp32Contents(values.get(2));

        ModelInferResponse.InferOutputTensor outputTensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP32")
                .addShape(1).addShape(3).setContents(contents).build();

        PredictionOutput predictionOutput = PayloadParser.outputTensorToPredictionOutput(outputTensor, null);

        assertEquals(3, predictionOutput.getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), Double.valueOf(predictionOutput.getOutputs().get(i).getValue().asNumber()).floatValue());
        }
    }

    @Test
    void modelInferResponseToPredictionOutputFp64() {

        final Random random = new Random();
        final List<Double> values = List.of(random.nextDouble(), random.nextDouble(), random.nextDouble());
        InferTensorContents.Builder contents = InferTensorContents.newBuilder()
                .addFp64Contents(values.get(0))
                .addFp64Contents(values.get(1))
                .addFp64Contents(values.get(2));

        ModelInferResponse.InferOutputTensor outputTensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(3).setContents(contents).build();

        PredictionOutput predictionOutput = PayloadParser.outputTensorToPredictionOutput(outputTensor, null);

        assertEquals(3, predictionOutput.getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), predictionOutput.getOutputs().get(i).getValue().asNumber());
        }
    }

    @Test
    void modelInferResponseToPredictionInputFp32() {

        final Random random = new Random();
        final List<Float> values = List.of(random.nextFloat(), random.nextFloat(), random.nextFloat());
        InferTensorContents.Builder contents = InferTensorContents.newBuilder()
                .addFp32Contents(values.get(0))
                .addFp32Contents(values.get(1))
                .addFp32Contents(values.get(2));

        ModelInferRequest.InferInputTensor tensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP32")
                .addShape(1).addShape(3).setContents(contents).build();

        PredictionInput predictionInput = PayloadParser.inputTensorToPredictionInput(tensor, null);

        assertEquals(3, predictionInput.getFeatures().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), Double.valueOf(predictionInput.getFeatures().get(i).getValue().asNumber()).floatValue());
        }
    }

    @Test
    void modelInferResponseToPredictionInputFp64() {

        final Random random = new Random();
        final List<Double> values = List.of(random.nextDouble(), random.nextDouble(), random.nextDouble());
        InferTensorContents.Builder contents = InferTensorContents.newBuilder()
                .addFp64Contents(values.get(0))
                .addFp64Contents(values.get(1))
                .addFp64Contents(values.get(2));

        ModelInferRequest.InferInputTensor tensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(3).setContents(contents).build();

        PredictionInput predictionInput = PayloadParser.inputTensorToPredictionInput(tensor, null);

        assertEquals(3, predictionInput.getFeatures().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), predictionInput.getFeatures().get(i).getValue().asNumber());
        }
    }
}