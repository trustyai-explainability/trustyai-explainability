package org.kie.trustyai.service.data.cache;

import java.lang.reflect.Method;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.kie.trustyai.service.data.storage.Storage;

import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CompositeCacheKey;

@ApplicationScoped
public class DataframeKeyGen implements CacheKeyGenerator {

    @Inject
    Instance<Storage> storage;

    @Override
    public Object generate(Method method, Object... methodParams) {
        return new CompositeCacheKey(storage.get().getLastModified());
    }
}
