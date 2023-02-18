package org.kie.trustyai.service.endpoints.consumer;

import java.util.Base64;

import javax.inject.Inject;
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
import org.kie.trustyai.explainability.model.*;
import org.kie.trustyai.service.data.DataSource;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.payloads.consumer.InferencePayload;

import com.google.protobuf.InvalidProtocolBufferException;

@Path("/consumer/kserve/v2")
public class ConsumerEndpoint {

    private static final Logger LOG = Logger.getLogger(ConsumerEndpoint.class);
    @Inject
    DataSource dataSource;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response consume(InferencePayload request) throws DataframeCreateException {
        LOG.info("Got payload on the consumer");
        try {
            final byte[] inputBytes = Base64.getDecoder().decode(request.input.getBytes());
            final ModelInferRequest input = ModelInferRequest.parseFrom(inputBytes);
            final PredictionInput predictionInput = PayloadParser.inputTensorToPredictionInput(input.getInputs(0), null);
            LOG.info(predictionInput.getFeatures());

            final byte[] outputBytes = Base64.getDecoder().decode(request.output.getBytes());
            final ModelInferResponse output = ModelInferResponse.parseFrom(outputBytes);
            final PredictionOutput predictionOutput = PayloadParser.outputTensorToPredictionOutput(output.getOutputs(0), null);
            LOG.info(predictionOutput.getOutputs());

            final Prediction prediction = new SimplePrediction(predictionInput, predictionOutput);

            final Dataframe dataframe = Dataframe.createFrom(prediction);

            dataSource.appendDataframe(dataframe);

        } catch (InvalidProtocolBufferException e) {
            LOG.error("Error parsing protobuf message: " + e.getMessage());
            return Response.serverError().status(500).build();
        }

        return Response.ok().build();
    }

}