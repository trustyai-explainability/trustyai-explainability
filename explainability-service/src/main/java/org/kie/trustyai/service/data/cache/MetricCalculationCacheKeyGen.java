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

    @Override
    public Object generate(Method method, Object... methodParams) {

        final BaseMetricRequest request = (BaseMetricRequest) methodParams[1];
        LOG.debug("Cache hit for metric for model " + request.getModelId());
        return new CompositeCacheKey(storage.get().getLastModified(request.getModelId()), request.hashCode());
    }
}
