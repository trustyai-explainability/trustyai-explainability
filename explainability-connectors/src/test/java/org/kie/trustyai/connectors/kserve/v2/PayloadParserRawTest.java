package org.kie.trustyai.connectors.kserve.v2;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for {@link PayloadParser} class with KServe raw contents
 */
class PayloadParserRawTest {

    @Test
    void modelInferRequestToPredictionOutputDoubleSingle() {

        final Random random = new Random();
        final double value = random.nextDouble();

        ModelInferRequest.Builder builder = ModelInferRequest.newBuilder();
        builder.addRawInputContents(RawConverter.fromDouble(List.of(value)));

        ModelInferRequest.InferInputTensor inputTensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(1).build();

        builder.addInputs(inputTensor);

        PredictionInput predictionInput = PayloadParser.requestToInput(builder.build(), null);
        //
        assertEquals(1, predictionInput.getFeatures().size());
        assertEquals(value, predictionInput.getFeatures().get(0).getValue().asNumber());
    }

    // Output tests
    @Test
    void modelInferResponseToPredictionOutputMulti() {

        final Random random = new Random();
        final List<Double> values = random.doubles(3).boxed().collect(Collectors.toList());

        ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addRawOutputContents(RawConverter.fromDouble(values));
        ModelInferResponse.InferOutputTensor outputTensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(3).build();
        response.addOutputs(outputTensor);

        PredictionOutput predictionOutput = PayloadParser.rawContentToPredictionOutput(response.build(), null);

        assertEquals(3, predictionOutput.getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), predictionOutput.getOutputs().get(i).getValue().asNumber());
        }
    }

    @Test
    void modelInferResponseToPredictionOutputFp32() {

        final Random random = new Random();
        final List<Float> values = List.of(random.nextFloat(), random.nextFloat(), random.nextFloat());
        ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addRawOutputContents(RawConverter.fromFloat(values));
        ModelInferResponse.InferOutputTensor outputTensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP32")
                .addShape(1).addShape(3).build();
        response.addOutputs(outputTensor);

        PredictionOutput predictionOutput = PayloadParser.rawContentToPredictionOutput(response.build(), null);

        assertEquals(3, predictionOutput.getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), Double.valueOf(predictionOutput.getOutputs().get(i).getValue().asNumber()).floatValue());
        }
    }

    @Test
    void modelInferResponseToPredictionOutputFp64() {

        final Random random = new Random();
        final List<Double> values = List.of(random.nextDouble(), random.nextDouble(), random.nextDouble());
        ModelInferResponse.Builder response = ModelInferResponse.newBuilder();
        response.addRawOutputContents(RawConverter.fromDouble(values));
        ModelInferResponse.InferOutputTensor outputTensor = ModelInferResponse.InferOutputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(3).build();
        response.addOutputs(outputTensor);

        PredictionOutput predictionOutput = PayloadParser.rawContentToPredictionOutput(response.build(), null);

        assertEquals(3, predictionOutput.getOutputs().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), predictionOutput.getOutputs().get(i).getValue().asNumber());
        }
    }

    @Test
    void modelInferResponseToPredictionInputFp32() {

        final Random random = new Random();
        final List<Float> values = List.of(random.nextFloat(), random.nextFloat(), random.nextFloat());
        ModelInferRequest.Builder builder = ModelInferRequest.newBuilder();
        builder.addRawInputContents(RawConverter.fromFloat(values));
        ModelInferRequest.InferInputTensor tensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP32")
                .addShape(1).addShape(3).build();
        builder.addInputs(tensor);

        PredictionInput predictionInput = PayloadParser.rawContentToPredictionInput(builder.build(), null);

        assertEquals(3, predictionInput.getFeatures().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), Double.valueOf(predictionInput.getFeatures().get(i).getValue().asNumber()).floatValue());
        }
    }

    @Test
    void modelInferResponseToPredictionInputFp64() {

        final Random random = new Random();
        final List<Double> values = List.of(random.nextDouble(), random.nextDouble(), random.nextDouble());
        ModelInferRequest.Builder builder = ModelInferRequest.newBuilder();
        builder.addRawInputContents(RawConverter.fromDouble(values));
        ModelInferRequest.InferInputTensor tensor = ModelInferRequest.InferInputTensor.newBuilder()
                .setDatatype("FP64")
                .addShape(1).addShape(3).build();
        builder.addInputs(tensor);

        PredictionInput predictionInput = PayloadParser.rawContentToPredictionInput(builder.build(), null);

        assertEquals(3, predictionInput.getFeatures().size());
        for (int i = 0; i < 3; i++) {
            assertEquals(values.get(i), predictionInput.getFeatures().get(i).getValue().asNumber());
        }
    }
}