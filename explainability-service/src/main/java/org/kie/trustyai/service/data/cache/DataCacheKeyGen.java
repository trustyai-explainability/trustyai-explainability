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

    /**
     * Generates a cache key based on a data file's last modified status.
     * If the data file for a certain model id has not changed, return the cached data.
     * 
     * @param method The method is {@link Storage#readData(String)}
     * @param methodParams The model id as a {@link String}
     * @return A key cache based on the model id's data file last modified attribute
     */
    @Override
    public Object generate(Method method, Object... methodParams) {
        final String modelId = (String) methodParams[0];
        LOG.debug("Creating cache key for " + modelId + " data");

        return new CompositeCacheKey(modelId, storage.get().getLastModified(modelId));
    }
}
