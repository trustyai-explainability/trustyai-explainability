package org.kie.trustyai.service.data.cache;

import java.lang.reflect.Method;
import java.util.Map;

import org.jboss.logging.Logger;
import org.kie.trustyai.service.payloads.service.Schema;

import io.quarkus.cache.CacheKeyGenerator;
import io.quarkus.cache.CompositeCacheKey;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SchemaNameMappingCacheKeyGen implements CacheKeyGenerator {

    private static final Logger LOG = Logger.getLogger(SchemaNameMappingCacheKeyGen.class);

    /**
     * Generates a cache key for schema name mapped items
     * 
     * @param method This refers to the calculation methods}.
     * @param methodParams Schema parameters
     * @return A composite cache key.
     */
    @Override
    public Object generate(Method method, Object... methodParams) {

        final Schema schema = (Schema) methodParams[0];
        Map<String, String> nameMapping = schema.getNameMapping();

        LOG.debug("Creating cache key for schema");
        return new CompositeCacheKey(nameMapping, schema.hashCode());
    }
}
