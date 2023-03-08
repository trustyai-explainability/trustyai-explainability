package org.kie.trustyai.service.data.cache;

import java.lang.reflect.Method;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.storage.Storage;
import org.kie.trustyai.service.payloads.BaseMetricRequest;

import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CompositeCacheKey;

@ApplicationScoped
public class MetricCalculationCacheKeyGen implements CacheKeyGenerator {

    private static final Logger LOG = Logger.getLogger(MetricCalculationCacheKeyGen.class);
    @Inject
    Instance<Storage> storage;

    /**
     * Generates a cache key for metric calculations.
     * If (for a specific model id) the data file is not modified and the metric request
     * was already calculated, this will result in a cache hit for that metric.
     * If either the data or the request are new, the metric will fully calculated.
     * 
     * @param method This refers to the calculation methods in {@link org.kie.trustyai.service.endpoints.metrics.MetricsCalculator}.
     * @param methodParams Metric calculation parameters. Only {@link BaseMetricRequest} is used.
     * @return A composite cache key.
     */
    @Override
    public Object generate(Method method, Object... methodParams) {

        final BaseMetricRequest request = (BaseMetricRequest) methodParams[1];
        final String modelId = request.getModelId();
        LOG.debug("Creating cache key for model " + modelId);
        return new CompositeCacheKey(modelId, storage.get().getLastModified(modelId), request.hashCode());
    }
}
