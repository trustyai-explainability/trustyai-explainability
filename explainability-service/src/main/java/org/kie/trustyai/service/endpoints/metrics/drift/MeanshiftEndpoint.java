package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.xml.crypto.Data;

import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.explainability.model.DatapointSource;
import org.kie.trustyai.metrics.drift.meanshift.Meanshift;
import org.kie.trustyai.metrics.drift.meanshift.MeanshiftFitting;
import org.kie.trustyai.metrics.drift.meanshift.MeanshiftResult;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.endpoints.service.ServiceMetadataEndpoint;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
import org.kie.trustyai.service.validators.metrics.ValidReconciledMetricRequest;
import org.kie.trustyai.service.validators.metrics.drift.ValidDriftMetricRequest;

import io.quarkus.cache.CacheResult;

@ApplicationScoped
@Tag(name = "Meanshift Drift Endpoint", description = "Meanshift measures that the columns of the tested dataframe come " +
        "from the same distribution as the training dataframe.")
@Path("/metrics/drift/meanshift")
public class MeanshiftEndpoint extends DriftEndpoint {
    public MeanshiftEndpoint() {
        super("MEANSHIFT");
    }

    private static final Logger LOG = Logger.getLogger(MeanshiftEndpoint.class);

    @Override
    public MetricThreshold thresholdFunction(Number delta, MetricValueCarrier metricValue) {
        return new MetricThreshold(
                0,
                delta.doubleValue(),
                metricValue.getNamedValues().values().stream().max(Comparator.naturalOrder()).orElse(0.));
    }

    @Override
    public String getGeneralDefinition() {
        return "MeanShift gives the per-column probability that the data values seen in a test dataset come from the same distribution of a training dataset, under the assumption that the values are normally distributed.";
    }

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


    @Override
    @CacheResult(cacheName = "metrics-calculator-meanshift", keyGenerator = MetricCalculationCacheKeyGen.class)
    public MetricValueCarrier calculate(Dataframe dataframe, @ValidReconciledMetricRequest BaseMetricRequest request) {
        @ValidDriftMetricRequest DriftMetricRequest dmRequest = (DriftMetricRequest) request;

        MeanshiftFitting msf;
        if (dmRequest.getFitting() == null) {
            LOG.info("Fitting a meanshift drift request");

            // get the data that matches the provided reference tag: calibration data
            Dataframe fitting = super.dataSource.get()
                    .getDataframe(request.getModelId())
                    .filterRowsByTagEquals(DatapointSource.valueOf(dmRequest.getReferenceTag()));
            msf = Meanshift.precompute(fitting);
            dmRequest.setFitting(msf.getFitStats());
        } else {
            LOG.debug("Using previously found fitting in request");
            msf = new MeanshiftFitting(dmRequest.getFitting());
        }
        Meanshift ms = new Meanshift(msf);
        LOG.debug("Cache miss. Calculating metric for " + dmRequest.getModelId());

        // get data that does _not_ have the provided reference tag: test data
        Dataframe filtered = dataframe.filterRowsByTagNotEquals(DatapointSource.valueOf(((DriftMetricRequest) request).getReferenceTag()));
        Map<String, MeanshiftResult> result = ms.calculate(filtered, dmRequest.getThresholdDelta());

        Map<String, Double> namedValues = new HashMap<>();
        for (Map.Entry<String, MeanshiftResult> resultEntry : result.entrySet()) {
            namedValues.put(resultEntry.getKey(), resultEntry.getValue().getpValue());
        }
        return new MetricValueCarrier(namedValues);
    }
}
