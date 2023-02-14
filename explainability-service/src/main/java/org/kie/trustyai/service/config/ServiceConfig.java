package org.kie.trustyai.service.config;

import java.util.Optional;
import java.util.OptionalInt;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "service", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface ServiceConfig {

    OptionalInt batchSize();

    String modelName();

    @WithName("optional.string")
    Optional<String> kserveTarget();

    String storageFormat();

    String dataFormat();

    String metricsSchedule();

}
