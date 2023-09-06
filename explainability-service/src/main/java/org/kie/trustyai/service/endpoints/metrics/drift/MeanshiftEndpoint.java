package org.kie.trustyai.service.endpoints.metrics.drift;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Path;

import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.kie.trustyai.explainability.model.Dataframe;
import org.kie.trustyai.metrics.drift.meanshift.Meanshift;
import org.kie.trustyai.metrics.drift.meanshift.MeanshiftFitting;
import org.kie.trustyai.metrics.drift.meanshift.MeanshiftResult;
import org.kie.trustyai.service.data.cache.MetricCalculationCacheKeyGen;
import org.kie.trustyai.service.payloads.data.statistics.ColumnSummaryPayload;
import org.kie.trustyai.service.payloads.metrics.BaseMetricRequest;
import org.kie.trustyai.service.payloads.metrics.MetricThreshold;
import org.kie.trustyai.service.payloads.metrics.drift.DriftMetricRequest;
import org.kie.trustyai.service.prometheus.MetricValueCarrier;
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

    @Override
    public MetricThreshold thresholdFunction(Number delta, Number metricValue) {
        if (delta == null) {
            return new MetricThreshold(
                    metricsConfig.meanshift().thresholdLower(),
                    metricsConfig.meanshift().thresholdUpper(),
                    metricValue.doubleValue());
        } else {
            return new MetricThreshold(
                    0,
                    delta.doubleValue(),
                    metricValue.doubleValue());
        }
    }

    private MeanshiftFitting convertToFitting(List<String> colNames, Map<String, ColumnSummaryPayload> payloadFitting) {
        Map<String, StatisticalSummaryValues> computedStats = new HashMap<>();
        for (Map.Entry<String, ColumnSummaryPayload> entry : payloadFitting.entrySet()) {
            if (!colNames.contains(entry.getKey())) {
                throw new IllegalArgumentException(String.format("Column %s from provided fitting not present in specified dataframe", entry.getKey()));
            }

            StatisticalSummaryValues ssv = new StatisticalSummaryValues(
                    entry.getValue().mean,
                    Math.pow(entry.getValue().stdDev, 2),
                    entry.getValue().nValues,
                    entry.getValue().max,
                    entry.getValue().min,
                    entry.getValue().sum);
            computedStats.put(entry.getKey(), ssv);
        }

        return new MeanshiftFitting(computedStats);
    }

    @Override
    @CacheResult(cacheName = "metrics-calculator-meanshift", keyGenerator = MetricCalculationCacheKeyGen.class)
    public MetricValueCarrier calculate(Dataframe dataframe, @ValidDriftMetricRequest BaseMetricRequest request) {
        DriftMetricRequest dmRequest = (DriftMetricRequest) request;
        MeanshiftFitting msf = convertToFitting(dataframe.getColumnNames(), dmRequest.getFitting());
        Meanshift ms = new Meanshift(msf);
        LOG.debug("Cache miss. Calculating metric for " + dmRequest.getModelId());
        Map<String, MeanshiftResult> result = ms.calculate(dataframe, dmRequest.getUpperThreshold());

        Map<String, Double> namedValues = new HashMap<>();
        for (Map.Entry<String, MeanshiftResult> resultEntry : result.entrySet()) {
            namedValues.put(resultEntry.getKey(), resultEntry.getValue().getpValue());
        }
        return new MetricValueCarrier(namedValues);
    }
}
