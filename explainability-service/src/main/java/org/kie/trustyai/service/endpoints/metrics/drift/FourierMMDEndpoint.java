package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMD;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMDFitting;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMDResult;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.data.exceptions.DataframeCreateException;
import org.kie.trustyai.service.data.exceptions.MetricCalculationException;
import org.kie.trustyai.service.endpoints.metrics.BaseEndpoint;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.BaseMetricResponse;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDMetricRequest;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDParameters;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Tag(name = "FourierMMD Drift Endpoint", description = "Meanshift measures the distance between distributions as " +
        "distance between mean embeddings of features from the test dataframe and the training dataframe.")
@Path("/metrics/drift/fouriermmd")
public class FourierMMDEndpoint extends BaseEndpoint<FourierMMDMetricRequest> {
    public FourierMMDEndpoint() {
        super("FOURIERMMD");
    }

    // === THRESHOLDS ======================================
    // determine if the metric value(s) exceed the provided threshold(s)
    public MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue) {
        return new MetricThreshold(
                0,
                delta.doubleValue(),
                metricValue.getNamedValues().values().stream().max(Comparator.naturalOrder()).orElse(0.));
    }

    // this function should provide a general definition for this class of metric
    public String getGeneralDefinition() {
        return "FourierMMD gives probability that the data values seen in a test dataset come from the same distribution of a training dataset, under the assumption that the computed mmd values are normally distributed.";
    }

    // this function should provide a specific definition/interpretation of what this specific metric value means
    public String getSpecificDefinition(MetricValueCarrier metricValues, FourierMMDMetricRequest request) {
        StringBuilder out = new StringBuilder(getGeneralDefinition());
        out.append(System.getProperty("line.separator"));

        Map<String, Double> namedValues = metricValues.getNamedValues();
        boolean isDrifted = namedValues.get("pValue") <= request.getThresholdDelta();
        out.append(String.format("  - Test data has p=%f probability of being drifted from the training distribution.",
                namedValues.get("pValue")));
        if (isDrifted) {
            out.append(String.format(" p > %f -> [SIGNIFICANT DRIFT]", request.getThresholdDelta()));
        } else {
            out.append(String.format(" p <=  %f", request.getThresholdDelta()));
        }

        return out.toString();

    }

    public MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request) {
        return calculate(dataframe, request);
    }

    // this function should provide the functionality of actually calculating a specific metric value for a given request
    @CacheResult(cacheName = "metrics-calculator-fouriermmd", keyGenerator = MetricCalculationCacheKeyGen.class)
    public MetricValueCarrier calculate(Dataframe dataframe, FourierMMDMetricRequest request) {
        FourierMMDMetricRequest fmmRequest = (FourierMMDMetricRequest) request;

        FourierMMDFitting fmf;
        if (fmmRequest.getFitting() == null) {
            LOG.debug("Fitting a fourier mmd drift request for model=" + request.getModelId());

            // get the data that matches the provided reference tag: calibration data
            Dataframe fitting = super.dataSource.get()
                    .getDataframe(request.getModelId())
                    .filterRowsByTagEquals(fmmRequest.getReferenceTag());

            // get parameters
            final FourierMMDParameters parameters = fmmRequest.getParameters();
            final int randomSeed = new Random().nextInt();

            fmf = FourierMMD.precompute(fitting,
                    parameters.getDeltaStat(),
                    parameters.getnTest(),
                    parameters.getnWindow(),
                    parameters.getSig(),
                    randomSeed,
                    parameters.getnMode());
            fmmRequest.setFitting(fmf.getFitStats());
        } else {
            LOG.debug("Using previously found fouriermmd fitting in request for model=" + request.getModelId());
            fmf = new FourierMMDFitting(fmmRequest.getFitting());
        }
        FourierMMD fmmd = new FourierMMD(fmf);
        LOG.debug("Cache miss. Calculating metric for " + fmmRequest.getModelId());

        // get data that does _not_ have the provided reference tag: test data
        Dataframe filtered = dataframe.filterRowsByTagNotEquals(((FourierMMDMetricRequest) request).getReferenceTag());
        FourierMMDResult result = fmmd.calculate(filtered, fmmRequest.getThresholdDelta(), fmmRequest.getGamma());

        Map<String, Double> namedValues = new HashMap<>();
        namedValues.put("pValue", result.getpValue());

        return new MetricValueCarrier(namedValues);
    }

    // returns the generic definition as a response object
    @Override
    @GET
    @Path("/definition")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefinition() {
        return Response.ok(getGeneralDefinition()).build();
    }

    // defines the request scheduling mechanism
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/request")
    public Response createRequest(FourierMMDMetricRequest request) {
        if (Objects.isNull(request.getThresholdDelta())) {
            final double defaultUpperThresh = metricsConfig.drift().thresholdDelta();
            request.setThresholdDelta(defaultUpperThresh);
        }
        return super.createRequestGeneric(request);
    }

    // == GLOBAL FUNCTIONS ======
    // this function defines the default individual request/response flow, for a single metric calculation at this exact timestamp
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response response(FourierMMDMetricRequest request) throws DataframeCreateException {

        final Dataframe dataframe;
        try {

            // fill request with default values if none are provided
            if (Objects.isNull(request.getBatchSize())) {
                final int defaultBatchSize = serviceConfig.batchSize().getAsInt();
                LOG.warn("Request batch size is empty. Using the default value of " + defaultBatchSize);
                request.setBatchSize(defaultBatchSize);
            }

            if (Objects.isNull(request.getThresholdDelta())) {
                final double defaultUpperThresh = metricsConfig.drift().thresholdDelta();
                request.setThresholdDelta(defaultUpperThresh);
            }

            // grab the slice of the requested data according to the provided batch size
            dataframe = super.dataSource.get().getDataframe(request.getModelId(), request.getBatchSize()).filterRowsBySynthetic(false);
        } catch (DataframeCreateException e) {
            LOG.error("No data available for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("No data available").build();
        }

        // use the calculate function to compute the metric value(s)
        final MetricValueCarrier metricValue;
        try {
            metricValue = this.calculate(dataframe, request);
        } catch (MetricCalculationException e) {
            LOG.error("Error calculating metric for model " + request.getModelId() + ": " + e.getMessage(), e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error calculating metric").build();
        }

        // get the metric definition and the threshold exceeded state
        final String metricDefinition = this.getSpecificDefinition(metricValue, request);
        MetricThreshold thresholds = thresholdFunction(request.getThresholdDelta(), metricValue);

        // wrap into response
        BaseMetricResponse response = new BaseMetricResponse(metricValue, metricDefinition, thresholds, super.getMetricName());
        return Response.ok(response).build();
    }
}
