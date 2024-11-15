package org.kie.trustyai.service.endpoints.consumer;

import java.nio.charset.StandardCharsets;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.reconcilers.KServeInferencePayloadReconciler;
import org.kie.trustyai.service.payloads.consumer.InferenceLoggerOutput;
import org.kie.trustyai.service.payloads.consumer.partial.KServeInputPayload;
import org.kie.trustyai.service.payloads.consumer.partial.KServeOutputPayload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CloudEventConsumer {

    private static final Logger LOG = Logger.getLogger(CloudEventConsumer.class);

    @Inject
    KServeInferencePayloadReconciler reconciler;

    @Funq
    @CloudEventMapping(trigger = "org.kubeflow.serving.inference.request")
    public void consumeKubeflowRequest(CloudEvent<byte[]> cloudEvent) {
        LOG.debug("Received Kubeflow request with id = " + cloudEvent.id());
        final KServeInputPayload input = new KServeInputPayload();
        input.setId(cloudEvent.id());
        input.setModelId(cloudEvent.extensions().get("Inferenceservicename"));
        input.setData(new String(cloudEvent.data(), StandardCharsets.UTF_8));
        reconciler.addUnreconciledInput(input);
    }

    @Funq
    @CloudEventMapping(trigger = "org.kubeflow.serving.inference.response")
    public void consumeKubeflowResponse(CloudEvent<byte[]> cloudEvent) throws JsonProcessingException {
        LOG.debug("Received Kubeflow response with id = " + cloudEvent.id());

        final KServeOutputPayload output = new KServeOutputPayload();
        output.setId(cloudEvent.id());
        output.setModelId(cloudEvent.extensions().get("Inferenceservicename"));
        byte[] original = cloudEvent.data();
        String decoded = new String(original, StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        InferenceLoggerOutput data = objectMapper.readValue(decoded, InferenceLoggerOutput.class);
        output.setData(data);
        reconciler.addUnreconciledOutput(output);
    }
}
