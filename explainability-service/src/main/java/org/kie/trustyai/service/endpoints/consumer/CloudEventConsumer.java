package org.kie.trustyai.service.endpoints.consumer;

import java.nio.charset.StandardCharsets;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.utils.KServeInferencePayloadReconciler;
import org.kie.trustyai.service.payloads.consumer.InferenceLoggerOutput;
import org.kie.trustyai.service.payloads.consumer.KServeInputPayload;
import org.kie.trustyai.service.payloads.consumer.KServeOutputPayload;

import io.quarkus.funqy.Funq;
import io.quarkus.funqy.knative.events.CloudEvent;
import io.quarkus.funqy.knative.events.CloudEventMapping;

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
    public void consumeKubeflowResponse(CloudEvent<InferenceLoggerOutput> cloudEvent) {
        LOG.debug("Received Kubeflow response with id = " + cloudEvent.id());
        final KServeOutputPayload output = new KServeOutputPayload();
        output.setId(cloudEvent.id());
        output.setModelId(cloudEvent.extensions().get("Inferenceservicename"));
        output.setData(cloudEvent.data());
        reconciler.addUnreconciledOutput(output);
    }
}