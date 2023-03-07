package org.kie.trustyai.service.data.cache;

import java.lang.reflect.Method;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.data.storage.Storage;

import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CompositeCacheKey;

@ApplicationScoped
public class DataCacheKeyGen implements CacheKeyGenerator {
    private static final Logger LOG = Logger.getLogger(DataCacheKeyGen.class);
    @Inject
    Instance<Storage> storage;

    @Override
    public Object generate(Method method, Object... methodParams) {
        final String modelId = (String) methodParams[0];
        LOG.debug("Cache hit for " + modelId + " data");

        return new CompositeCacheKey(storage.get().getLastModified(modelId));
    }
}
