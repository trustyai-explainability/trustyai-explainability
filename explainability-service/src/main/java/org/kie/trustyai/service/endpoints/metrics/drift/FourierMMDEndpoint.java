package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.metrics.drift.HypothesisTestResult;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMD;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMDFitting;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDMetricRequest;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDParameters;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.drift.ValidDriftMetricRequest;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Tag(name = "FourierMMD Drift Endpoint", description = "Meanshift measures the distance between distributions as " +
        "distance between mean embeddings of features from the test dataframe and the training dataframe.")
@Path("/metrics/drift/fouriermmd")
public class FourierMMDEndpoint extends DriftEndpoint<FourierMMDMetricRequest> {
    final Random rng = new Random();

    public FourierMMDEndpoint() {
        super("FOURIERMMD");
    }

    // === THRESHOLDS ======================================
    // determine if the metric value(s) exceed the provided threshold(s)
    @Override
    public MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue) {
        return new MetricThreshold(
                0,
                delta.doubleValue(),
                metricValue.getValue());
    }

    // this function should provide a general definition for this class of metric
    @Override
    public String getGeneralDefinition() {
        return "FourierMMD gives probability that the data values seen in a test dataset come from the same distribution of a training dataset, under the assumption that the computed mmd values are normally distributed.";
    }

    // this function should provide a specific definition/interpretation of what this specific metric value means
    @Override
    public String getSpecificDefinition(MetricValueCarrier metricValues, @ValidDriftMetricRequest FourierMMDMetricRequest request) {
        StringBuilder out = new StringBuilder(getGeneralDefinition());
        out.append(System.getProperty("line.separator"));

        double pValue = metricValues.getValue();
        boolean isDrifted = pValue <= request.getThresholdDelta();
        out.append(
                String.format(
                        "  - Test data has p=%f probability of being drifted from the training distribution.",
                        pValue));
        if (isDrifted) {
            out.append(String.format(" p > %f -> [SIGNIFICANT DRIFT]", request.getThresholdDelta()));
        } else {
            out.append(String.format(" p <=  %f", request.getThresholdDelta()));
        }

        return out.toString();

    }

    // this function should provide the functionality of actually calculating a specific metric value for a given request
    @CacheResult(cacheName = "metrics-calculator-fouriermmd", keyGenerator = MetricCalculationCacheKeyGen.class)
    public MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest bmRequest) {
        @ValidDriftMetricRequest
        FourierMMDMetricRequest request = (FourierMMDMetricRequest) bmRequest;

        FourierMMDFitting fmf;
        if (request.getFitting() == null) {
            LOG.debug("Fitting a fourier mmd drift request for model=" + request.getModelId());

            // get the data that matches the provided reference tag: calibration data
            Dataframe fitting = super.dataSource.get()
                    .getDataframe(request.getModelId())
                    .filterRowsByTagEquals(request.getReferenceTag());

            // get parameters
            final FourierMMDParameters parameters = request.getParameters();

            fmf = FourierMMD.precompute(fitting,
                    parameters.getDeltaStat(),
                    parameters.getnTest(),
                    parameters.getnWindow(),
                    parameters.getSig(),
                    rng.nextInt(),
                    parameters.getnMode());
            request.setFitting(fmf);
        } else {
            LOG.debug("Using previously found fouriermmd fitting in request for model=" + request.getModelId());
            fmf = request.getFitting();
        }
        FourierMMD fmmd = new FourierMMD(fmf);
        LOG.debug("Cache miss. Calculating metric for " + request.getModelId());

        // get data that does _not_ have the provided reference tag: test data
        Dataframe filtered = dataframe.filterRowsByTagNotEquals(request.getReferenceTag());
        HypothesisTestResult result = fmmd.calculate(filtered, request.getThresholdDelta(), request.getGamma());
        return new MetricValueCarrier(result.getpValue());
    }

    // returns the generic definition as a response object
    @Override
    @GET
    @Path("/definition")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDefinition() {
        return Response.ok(getGeneralDefinition()).build();
    }
}
