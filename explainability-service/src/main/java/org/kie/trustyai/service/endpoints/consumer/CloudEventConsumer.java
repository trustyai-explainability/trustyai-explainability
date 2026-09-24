package org.kie.trustyai.service.endpoints.consumer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

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
        byte[] data;
        try {
            data = cloudEvent.data();
        } catch (NullPointerException e) {
            LOG.warn("CloudEvent request has no data payload, skipping id=" + cloudEvent.id());
            return;
        }
        final KServeInputPayload input = new KServeInputPayload();
        input.setId(cloudEvent.id());
        input.setModelId(cloudEvent.extensions().get("Inferenceservicename"));
        input.setData(new String(decompressIfGzip(data), StandardCharsets.UTF_8));
        reconciler.addUnreconciledInput(input);
    }

    @Funq
    @CloudEventMapping(trigger = "org.kubeflow.serving.inference.response")
    public void consumeKubeflowResponse(CloudEvent<byte[]> cloudEvent) throws JsonProcessingException {
        LOG.debug("Received Kubeflow response with id = " + cloudEvent.id());
        byte[] data;
        try {
            data = cloudEvent.data();
        } catch (NullPointerException e) {
            LOG.warn("CloudEvent response has no data payload, skipping id=" + cloudEvent.id());
            return;
        }

        final KServeOutputPayload output = new KServeOutputPayload();
        output.setId(cloudEvent.id());
        output.setModelId(cloudEvent.extensions().get("Inferenceservicename"));
        byte[] original = decompressIfGzip(data);
        String decoded = new String(original, StandardCharsets.UTF_8);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        InferenceLoggerOutput loggerOutput = objectMapper.readValue(decoded, InferenceLoggerOutput.class);
        output.setData(loggerOutput);
        reconciler.addUnreconciledOutput(output);
    }

    static final int MAX_DECOMPRESSED_SIZE = 100 * 1024 * 1024; // 100 MB

    static byte[] decompressIfGzip(byte[] data) {
        if (data == null || data.length < 2 || (data[0] & 0xFF) != 0x1F || (data[1] & 0xFF) != 0x8B) {
            return data != null ? data : new byte[0];
        }
        try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data));
                ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
                if (bos.size() > MAX_DECOMPRESSED_SIZE) {
                    throw new IOException("Decompressed payload exceeds " + MAX_DECOMPRESSED_SIZE + " bytes");
                }
            }
            LOG.debug("Decompressed gzip CloudEvent payload");
            return bos.toByteArray();
        } catch (IOException e) {
            LOG.warn("CloudEvent payload starts with gzip magic bytes but failed to decompress, using raw bytes", e);
            return data;
        }
    }
}
