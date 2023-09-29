package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.metrics.drift.HypothesisTestResult;
import org.kie.trustyai.metrics.drift.kstest.KSTest;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.kstest.KSTestMetricRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.drift.ValidDriftMetricRequest;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Tag(name = "KSTest Drift Endpoint", description = "Kolmogorov-Smirnov Test measures the columns of the tested dataframe come " +
        "from the same distribution as the training dataframe.")
@Path("/metrics/drift/kstest")
public class KSTestEndpoint extends DriftEndpoint<KSTestMetricRequest> {
    public KSTestEndpoint() {
        super("KSTEST");
    }

    private static final Logger LOG = Logger.getLogger(KSTestEndpoint.class);

    // === THRESHOLDS ======================================
    // determine if the metric value(s) exceed the provided threshold(s)
    @Override
    public MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue) {
        return new MetricThreshold(
                0,
                delta.doubleValue(),
                metricValue.getNamedValues().values().stream().max(Comparator.naturalOrder()).orElse(0.));
    }

    @Override
    public String getGeneralDefinition() {
        return "KSTest calculates two sample kolmogorov-smirnov test per column which tests if two samples drawn from the same distributions.";
    }

    // a specific definition for this value of this metric in this specific context
    @Override
    public String getSpecificDefinition(MetricValueCarrier metricValues, @ValidDriftMetricRequest KSTestMetricRequest request) {
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

    @Override
    @CacheResult(cacheName = "metrics-calculator-kstest", keyGenerator = MetricCalculationCacheKeyGen.class)
    public MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request) {
        KSTestMetricRequest ksRequest = (KSTestMetricRequest) request;
        KSTest ks = new KSTest();
        // Get train data
        Dataframe trainData = super.dataSource.get()
                .getDataframe(request.getModelId())
                .filterRowsByTagEquals(ksRequest.getReferenceTag());
        // get data that does _not_ have the provided reference tag: test data
        Dataframe filtered = dataframe.filterRowsByTagNotEquals(ksRequest.getReferenceTag());
        if (filtered.getRowDimension() < 2) {
            LOG.warn("Test data has less than two observations; KSTest results will not be numerically reliable.");
        }
        LOG.debug("Cache miss. Calculating metric for " + ksRequest.getModelId());
        Map<String, HypothesisTestResult> result = ks.calculate(trainData, filtered, ksRequest.getThresholdDelta());

        Map<String, Double> namedValues = new HashMap<>();
        for (Map.Entry<String, HypothesisTestResult> resultEntry : result.entrySet()) {
            namedValues.put(resultEntry.getKey(), resultEntry.getValue().getpValue());
        }
        return new MetricValueCarrier(namedValues);
    }

}
