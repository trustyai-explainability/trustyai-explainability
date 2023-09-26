package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMD;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMDFitting;
import org.kie.trustyai.metrics.drift.fouriermmd.FourierMMDResult;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.fouriermmd.FourierMMDMetricRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.drift.ValidDriftMetricRequest;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Tag(name = "FourierMMD Drift Endpoint", description = "Meanshift measures the distance between distributions as " +
        "distance between mean embeddings of features from the test dataframe and the training dataframe.")
@Path("/metrics/drift/fouriermmd")
public class FourierMMDEndpoint extends DriftEndpoint {
    public FourierMMDEndpoint() {
        super("FOURIERMMD");
    }

    private static final Logger LOG = Logger.getLogger(FourierMMDEndpoint.class);

    // === THRESHOLDS ======================================
    // determine if the metric value(s) exceed the provided threshold(s)
    @Override
    public MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue) {
        return new MetricThreshold(
                0,
                delta.doubleValue(),
                metricValue.getNamedValues().values().stream().max(Comparator.naturalOrder()).orElse(0.));
    }

    // === DEFINITIONS ======================================
    // a generalized definition of this category of metric
    @Override
    public String getGeneralDefinition() {
        return "FourierMMD gives probability that the data values seen in a test dataset come from the same distribution of a training dataset, under the assumption that the computed mmd values are normally distributed.";
    }

    // a specific definition for this value of this metric in this specific context
    @Override
    public String getSpecificDefinition(MetricValueCarrier metricValues, @ValidDriftMetricRequest DriftMetricRequest request) {
        StringBuilder out = new StringBuilder(getGeneralDefinition());
        out.append(System.getProperty("line.separator"));

        int maxColLen = metricValues.getNamedValues().keySet().stream().mapToInt(s -> s.length()).max().orElse(0);
        String fmt = String.format("%%%ds", maxColLen);

        for (Map.Entry<String, Double> entry : metricValues.getNamedValues().entrySet()) {
            boolean reject = entry.getValue() <= request.getThresholdDelta();
            out.append(String.format("  - Column %s has p=%f probability of coming from the training distribution.",
                    String.format(fmt, entry.getKey()),
                    entry.getValue()));
            if (reject) {
                out.append(String.format(" p <= %f -> [SIGNIFICANT DRIFT]", request.getThresholdDelta()));
            } else {
                out.append(String.format(" p >  %f", request.getThresholdDelta()));
            }
            out.append(System.getProperty("line.separator"));
        }
        return out.toString();

    }

    // === CALCULATION FUNCTION ======================================
    @Override
    @CacheResult(cacheName = "metrics-calculator-meanshift", keyGenerator = MetricCalculationCacheKeyGen.class)
    public MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request) {
        DriftMetricRequest dmRequest = (DriftMetricRequest) request;

        MeanshiftFitting msf;
        if (dmRequest.getFitting() == null) {
            LOG.debug("Fitting a meanshift drift request for model=" + request.getModelId());

            // get the data that matches the provided reference tag: calibration data
            Dataframe fitting = super.dataSource.get()
                    .getDataframe(request.getModelId())
                    .filterRowsByTagEquals(dmRequest.getReferenceTag());
            msf = Meanshift.precompute(fitting);
            dmRequest.setFitting(msf.getFitStats());
        } else {
            LOG.debug("Using previously found meanshift fitting in request for model=" + request.getModelId());
            msf = new MeanshiftFitting(dmRequest.getFitting());
        }
        Meanshift ms = new Meanshift(msf);
        LOG.debug("Cache miss. Calculating metric for " + dmRequest.getModelId());

        // get data that does _not_ have the provided reference tag: test data
        Dataframe filtered = dataframe.filterRowsByTagNotEquals(((DriftMetricRequest) request).getReferenceTag());
        Map<String, MeanshiftResult> result = ms.calculate(filtered, dmRequest.getThresholdDelta());

        Map<String, Double> namedValues = new HashMap<>();
        for (Map.Entry<String, MeanshiftResult> resultEntry : result.entrySet()) {
            namedValues.put(resultEntry.getKey(), resultEntry.getValue().getpValue());
        }
        return new MetricValueCarrier(namedValues);
    }
}
