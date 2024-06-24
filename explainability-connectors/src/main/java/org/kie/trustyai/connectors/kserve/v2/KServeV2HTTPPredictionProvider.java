package org.kie.trustyai.connectors.kserve.v2;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.kie.trustyai.connectors.kserve.AbstractKServePredictionProvider;
import org.kie.trustyai.connectors.kserve.KServeDatatype;
import org.kie.trustyai.explainability.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Wraps a KServe v2-compatible HTTP model server as a TrustyAI {@link PredictionProvider}
 */
public class KServeV2HTTPPredictionProvider extends AbstractKServePredictionProvider implements PredictionProvider {

    private static final Logger logger = LoggerFactory.getLogger(KServeV2HTTPPredictionProvider.class);
    private final HttpClient httpClient;
    private final String endpointUrl;
    private final int outputShape;

    public KServeV2HTTPPredictionProvider(String inputName, List<String> outputNames, String endpointUrl) {
        this(inputName, outputNames, endpointUrl, 1);
    }

    public KServeV2HTTPPredictionProvider(String inputName, List<String> outputNames, String endpointUrl, int outputShape) {
        super(outputNames, inputName);
        this.httpClient = HttpClient.newHttpClient();
        this.endpointUrl = endpointUrl;
        this.outputShape = outputShape;
    }


    public CompletableFuture<List<PredictionOutput>> predictAsync(List<PredictionInput> inputs) {
        final ObjectMapper mapper = new ObjectMapper();

        final List<List<Double>> instances = inputs.stream()
                .map(input -> input.getFeatures().stream()
                        .map(Feature::getValue).map(Value::asNumber)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        final KServeV2RequestPayload.Inputs kinputs = new KServeV2RequestPayload.Inputs();
        kinputs.data = instances;
        kinputs.name = this.inputName;
        kinputs.shape = List.of(instances.size(), instances.get(0).size());
        kinputs.datatype = KServeDatatype.FP64;
        final KServeV2RequestPayload wrapper = new KServeV2RequestPayload(List.of(kinputs));

        // Convert to JSON
        try {
            final String json = mapper.writeValueAsString(wrapper);
            logger.debug("Sending " + json + " to " + endpointUrl);
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpointUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(response -> {
                        try {
                            return KServeV2HTTPPayloadParser.getInstance().parseResponse(response, outputShape);
                        } catch (JsonProcessingException e) {
                            throw new CompletionException(e);
                        }
                    })
                    .handle((result, throwable) -> {
                        if (throwable != null) {
                            logger.error("Error processing response: " + throwable.getMessage());
                            throw new RuntimeException("Error parsing server response", throwable);
                        }
                        return result;
                    });

        } catch (JsonProcessingException e) {
            final CompletableFuture<List<PredictionOutput>> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

}
