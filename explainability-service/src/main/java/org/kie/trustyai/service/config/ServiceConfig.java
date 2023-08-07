package org.kie.trustyai.service.config;

import java.util.OptionalInt;

import org.kie.trustyai.service.data.storage.DataFormat;
import org.kie.trustyai.service.data.storage.StorageFormat;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "service", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface ServiceConfig {

    OptionalInt batchSize();

    StorageFormat storageFormat();

    DataFormat dataFormat();

    String metricsSchedule();

}
