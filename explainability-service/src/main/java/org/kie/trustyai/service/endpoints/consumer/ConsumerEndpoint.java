package org.kie.trustyai.service.endpoints.consumer;

import java.util.Base64;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.kie.trustyai.connectors.kserve.v2.PayloadParser;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferRequest;
import org.kie.trustyai.connectors.kserve.v2.grpc.ModelInferResponse;
import org.kie.trustyai.explainability.model.PredictionInput;
import org.kie.trustyai.explainability.model.PredictionOutput;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;

import com.google.protobuf.InvalidProtocolBufferException;

@Path("/consumer")
public class ConsumerEndpoint {

    private static final Logger LOG = Logger.getLogger(ConsumerEndpoint.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response consume(InferencePayload request) throws DataframeCreateException {
        LOG.info("Got payload on the consumer");
        try {
            byte[] inputBytes = Base64.getDecoder().decode(request.input.getBytes());
            ModelInferRequest input = ModelInferRequest.parseFrom(inputBytes);
            PredictionInput predictionInput = PayloadParser.inputTensorToPredictionInput(input.getInputs(0), null);
            LOG.info(predictionInput.getFeatures());
            byte[] outputBytes = Base64.getDecoder().decode(request.output.getBytes());
            ModelInferResponse output = ModelInferResponse.parseFrom(outputBytes);
            PredictionOutput predictionOutput = PayloadParser.outputTensorToPredictionOutput(output.getOutputs(0), null);
            LOG.info(predictionOutput.getOutputs());
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

        return Response.ok().build();
    }

}
